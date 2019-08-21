package org.golemites.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.tools.jib.api.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1DeploymentList;
import io.kubernetes.client.models.V1DeploymentSpec;
import io.kubernetes.client.models.V1LabelSelector;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import okio.BufferedSink;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.golemites.api.DelayedBuilder;
import org.golemites.api.Dependency;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.PushTarget;
import org.golemites.api.TargetPlatformSpec;
import org.golemites.autobundle.AutoBundleSupport;
import org.golemites.launcher.Launcher;
import org.golemites.repository.ClasspathRepositoryStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import static org.golemites.repository.ClasspathRepositoryStore.BLOB_FILENAME;

/**
 * OCI-Image Assembler. No-Fatjar.
 */
public class ImageBuilder {
    private final static Logger LOG = LoggerFactory.getLogger(ImageBuilder.class);

    private static final String BASE_IMAGE = "openjdk:8-jre-alpine";
    private static final String JAVA_PATH = "/usr/bin/java";

    private final GolemitesApplicationExtension config;

    public ImageBuilder(GolemitesApplicationExtension config) {
        this.config = config;
    }

    public TargetPlatformSpec build(File output, URI launcher, TargetPlatformSpec inputSpec, List<URI> applicationDependencies) {
        try {
            File targetBase = output.getParentFile();
            targetBase.mkdirs();

            List<Path> deps = new ArrayList<>();
            for (Dependency d : inputSpec.getDependencies()) {
                File parent = new File(targetBase,"PLATFORM");
                parent.mkdirs();
                File f = new File(parent, d.getIdentity() + ".jar");
                try (InputStream in = d.getLocation().toURL().openStream(); BufferedSink sink = Okio.buffer(Okio.sink(f))) {
                    sink.writeAll(Okio.source(in));
                }
                LOG.info("Writing " + d.getLocation() + " to PLATFORM/" + f.getName());

                d.setLocation(URI.create("file:///PLATFORM/" + f.getName()));
                deps.add(f.toPath());
            }

            List<Dependency> appDeps = calculateAutobundles(applicationDependencies);
            List<Path> appPaths = new ArrayList<>();
            for (Dependency d : appDeps) {
                File localTarget = new File(targetBase,"APPLICATION/" + d.getIdentity() + ".jar");
                localTarget.getParentFile().mkdirs();
                try (InputStream in = d.getLocation().toURL().openStream(); BufferedSink sink = Okio.buffer(Okio.sink(localTarget))) {
                    sink.writeAll(Okio.source(in));
                }
                d.setLocation(URI.create("file:///APPLICATION/" + d.getIdentity() + ".jar"));
                appPaths.add(localTarget.toPath());
            }

            List<Dependency> allDeps = new ArrayList<>(Arrays.asList(inputSpec.getDependencies()));
            //List<Path> appPaths = appDeps.stream().map((dep) -> new File(dep.getLocation()).toPath()).collect(Collectors.toList());
            allDeps.addAll(appDeps);
            inputSpec.setDependencies(allDeps.toArray(new Dependency[0]));

            // Write final launcher with spec included.
            assembleJar(output, launcher, inputSpec);
            File specPath = new File(targetBase,"CONFIGURATION/" +BLOB_FILENAME);
            specPath.getParentFile().mkdirs();
            writeBlob(inputSpec,specPath);

            if (config.isDeployImage()) {
                // BUILD IMAGE
                JibContainerBuilder containerBuilder = Jib.from(BASE_IMAGE);
                Path launcherPath = new File(launcher).toPath();
                containerBuilder.addLayer(Collections.singletonList(launcherPath), AbsoluteUnixPath.get("/"));
                containerBuilder.addLayer(deps, AbsoluteUnixPath.get("/PLATFORM"));
                containerBuilder.addLayer(appPaths, AbsoluteUnixPath.get("/APPLICATION"));
                containerBuilder.addLayer(Collections.singletonList(specPath.toPath()), AbsoluteUnixPath.get("/CONFIGURATION"));

                containerBuilder.setCreationTime(Instant.now());
                containerBuilder.setEntrypoint(JAVA_PATH, "-jar", "/" + launcherPath.getFileName().toString());
                ImageReference ref = ImageReference.parse(config.getRepository());
                // Stack it all together:
                if (config.getPushTo() == PushTarget.REGISTRY) {
                    JibContainer result = containerBuilder
                            .containerize(Containerizer.to(RegistryImage.named(ref)
                                    .addCredentialRetriever(CredentialRetrieverFactory.forImage(ref).dockerConfig())));
                    inputSpec.setImageID(result.getDigest().getHash());
                } else if (config.getPushTo() == PushTarget.DOCKER_DAEMON) {
                    JibContainer result = containerBuilder.containerize(Containerizer.to(DockerDaemonImage.named(ref)));
                    inputSpec.setImageID(result.getDigest().getHash());
                } else {
                     JibContainer result = containerBuilder.containerize(Containerizer.to(TarImage.named(ref).saveTo(new File(targetBase, config.getName() + "-image.tar.gz").toPath())));
                     inputSpec.setImageID(result.getDigest().getHash());
                }
            }else {
                LOG.info("Won't build image because of user configuration.");
            }
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(inputSpec));

            return inputSpec;
        }catch (Exception e) {
            throw new RuntimeException("Problem!",e);
        }
    }

    public void deploy(String imageId) throws IOException, ApiException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        AppsV1Api appsApi = new AppsV1Api();

        // create:
        V1Deployment deployment = createDeployment(imageId);
        LOG.info(Yaml.dump(deployment));
        Optional<V1Deployment> oldDeployment = existingDeployment(appsApi);
        if (oldDeployment.isPresent()) {
            LOG.info("Replace existing configuration:" + oldDeployment);
            appsApi.replaceNamespacedDeployment(config.getName(),config.getNamespace(),deployment,null,null);
        }else {
            LOG.info("Fresh deployment: " + deployment.getMetadata().getName());
            V1Deployment result = appsApi.createNamespacedDeployment(config.getNamespace(), deployment, false, null, null);
        }
    }

    private Optional<V1Deployment> existingDeployment(AppsV1Api appsApi) {
        // TODO: replace with proper find on labels:
        //V1DeploymentList oldDeployments = appsApi.listNamespacedDeployment(config.getNamespace(),true,null,null,"app=" + config.getName(),null,1,null,10,false);
        try {
            return Optional.of(appsApi.readNamespacedDeployment(config.getName(),config.getNamespace(),null,false,false));
        } catch (ApiException e) {
            return Optional.empty();
        }
    }

    private V1Deployment createDeployment(String imageId) {
        V1Deployment deployment = new V1Deployment();
        V1ObjectMeta metadata = new V1ObjectMeta();
        deployment.setApiVersion("apps/v1");
        metadata.setName(config.getName());
        deployment.setMetadata(metadata);
        deployment.setKind("Deployment");
        V1DeploymentSpec spec = new V1DeploymentSpec();
        V1LabelSelector selector = new V1LabelSelector();
        selector.setMatchLabels(Collections.singletonMap("app", config.getName()));
        spec.setSelector(selector);
        spec.setReplicas(1);
        V1PodTemplateSpec template = new V1PodTemplateSpec();
        V1ObjectMeta templateMetadata = new V1ObjectMeta();
        templateMetadata.setLabels(Collections.singletonMap("app",config.getName()));
        template.setMetadata(templateMetadata);

        V1PodSpec podSpec = new V1PodSpec();
        List<V1Container> containers = new ArrayList<>();
        V1Container container = new V1Container();
        containers.add(container);
        container.setName(config.getName());
        container.setImage(config.getRepository() + imageId);
        //container.setImagePullPolicy("Always");
        V1ContainerPort containerPort = new V1ContainerPort();
        containerPort.setContainerPort(8080);
        container.setPorts(Collections.singletonList(containerPort));
        podSpec.setContainers(containers);
        template.setSpec(podSpec);
        spec.setTemplate(template);
        deployment.setSpec(spec);
        return deployment;
    }

    private void assembleJar(File output, URI launcher, TargetPlatformSpec inputSpec) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Launcher.class.getName());
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(output), manifest)) {
            Set<String> ignore = new HashSet<>(Arrays.asList("META-INF/DEPENDENCIES", "META-INF/MANIFEST.MF", "META-INF/LICENSE", "META-INF/NOTICE"));
            writeLauncherDeps(launcher, ignore, jos);
            writeBlob(inputSpec, jos);
        }
    }

    private void writeBlob(TargetPlatformSpec targetPlatformSpec, JarOutputStream jos) throws IOException {
        jos.putNextEntry(new JarEntry(BLOB_FILENAME));
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(jos, targetPlatformSpec);
    }

    private void writeBlob(TargetPlatformSpec targetPlatformSpec, File here) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (FileOutputStream fos = new FileOutputStream(here)) {
            mapper.writeValue(fos, targetPlatformSpec);
        }
    }

    private List<Dependency> calculateAutobundles(List<URI> projectConf) throws IOException, URISyntaxException {
        AutoBundleSupport autobundle = new AutoBundleSupport();
        List<URL> potentialAutobundles = new ArrayList<>();
        for (URI art : projectConf) {
            LOG.info("Found Artifact for autobundle: " + art.toASCIIString());
            potentialAutobundles.add(art.toURL());
        }

        Set<DelayedBuilder<Dependency>> result = autobundle.discover(potentialAutobundles);
        List<Dependency> deps = new ArrayList<>();
        for (DelayedBuilder<Dependency> auto : result) {
            Dependency bundle = auto.build();
            LOG.info("Writing Autobundle to Fatjar: " + bundle);
            deps.add(bundle);
        }
        return deps;
    }

    public TargetPlatformSpec findSpec(List<URI> c) throws IOException {
        for (URI artifact : c) {
            // here we first need to discover and resolve the existing blob file:
            LOG.info("++ Project artifact: " + artifact.toASCIIString());
            // find blobs:
            File f = new File(artifact);
            if (f.isFile()) {
                try (JarInputStream jis = new JarInputStream(new FileInputStream(f))) {
                    JarEntry entry = null;
                    while ((entry = jis.getNextJarEntry()) != null) {
                        LOG.debug("Checking " + entry.getName() + " ---- ");
                        if (BLOB_FILENAME.equals(entry.getName())) {
                            return new ClasspathRepositoryStore(Okio.buffer(Okio.source(jis)).readByteArray()).platform();
                        }
                        LOG.debug("Writing entry: " + entry.getName());
                    }
                }
            }
        }
        throw new RuntimeException("Spec not found!");
    }

    private void writeLauncherDeps(URI launcher, Set<String> ignore, JarOutputStream jos) throws IOException {
        try (JarInputStream jis = new JarInputStream(launcher.toURL().openStream())) {
            JarEntry entry = null;
            while ((entry = jis.getNextJarEntry()) != null) {
                LOG.debug("Writing entry: " + entry.getName());
                if (!ignore.contains(entry.getName())) {
                    try {
                        jos.putNextEntry(entry);
                        byte[] buffer = new byte[1024];
                        while (true) {
                            int count = jis.read(buffer);
                            if (count == -1)
                                break;
                            jos.write(buffer, 0, count);
                        }
                    } catch (ZipException ze) {
                        LOG.debug("Duplicate entry " + ze.getMessage());
                    }

                }
            }
        }
    }
}
