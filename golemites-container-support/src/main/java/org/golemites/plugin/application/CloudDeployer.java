package org.golemites.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.tools.jib.api.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1DeploymentSpec;
import io.kubernetes.client.models.V1HTTPGetAction;
import io.kubernetes.client.models.V1LabelSelector;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1Probe;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import org.golemites.api.Dependency;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.PushTarget;
import org.golemites.api.TargetPlatformSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.golemites.repository.ClasspathRepositoryStore.BLOB_FILENAME;

public class CloudDeployer {
    private final static Logger LOG = LoggerFactory.getLogger(CloudDeployer.class);

    static final String BASE_IMAGE = "openjdk:8-jre-alpine";
    static final String JAVA_PATH = "/usr/bin/java";
    private final Path sourceBase;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Path launcherPath;
    private final GolemitesApplicationExtension config;

    public CloudDeployer(Path launcher, Path sourceBase, GolemitesApplicationExtension config) {
        this.launcherPath = launcher;
        this.sourceBase = sourceBase;
        this.config = config;
    }

    public String createImage(Path targetBase) throws InvalidImageReferenceException, IOException, InterruptedException, RegistryException, CacheDirectoryCreationException, ExecutionException {
        JibContainerBuilder containerBuilder = Jib.from(BASE_IMAGE);
        // read spec
        Path specPath = TargetPlatformSpec.configuration(sourceBase).resolve(BLOB_FILENAME);
        TargetPlatformSpec spec = mapper.readValue(Files.readAllBytes(specPath), TargetPlatformSpec.class);
        LOG.info("Copy " + launcherPath + " to " + sourceBase);
        containerBuilder.addLayer(Collections.singletonList(launcherPath), AbsoluteUnixPath.fromPath(targetBase));
        containerBuilder.addLayer(mapToPaths(spec.getDependencies()), AbsoluteUnixPath.fromPath(TargetPlatformSpec.platformPath(targetBase)));
        containerBuilder.addLayer(mapToPaths(spec.getApplication()), AbsoluteUnixPath.fromPath(TargetPlatformSpec.applicationPath(targetBase)));
        containerBuilder.addLayer(Collections.singletonList(specPath), AbsoluteUnixPath.fromPath(TargetPlatformSpec.configuration(targetBase)));

        containerBuilder.setCreationTime(Instant.now());
        containerBuilder.setEntrypoint(JAVA_PATH, "-jar", "/" + launcherPath.getFileName().toString(), "run");
        return deployImage(containerBuilder);
    }

    private String deployImage(JibContainerBuilder containerBuilder) throws InvalidImageReferenceException, InterruptedException, RegistryException, IOException, CacheDirectoryCreationException, ExecutionException {
        ImageReference ref = ImageReference.parse(config.getRepository());
        // Stack it all together:
        if (config.getPushTo() == PushTarget.REGISTRY) {
            JibContainer result = containerBuilder.containerize(Containerizer.to(RegistryImage.named(ref)
                    .addCredentialRetriever(CredentialRetrieverFactory.forImage(ref).dockerConfig())));
            return result.getDigest().getHash();
        } else if (config.getPushTo() == PushTarget.DOCKER_DAEMON) {
            JibContainer result = containerBuilder.containerize(Containerizer.to(DockerDaemonImage.named(ref)));
            return result.getDigest().getHash();
        } else {
            JibContainer result = containerBuilder.containerize(Containerizer.to(TarImage.named(ref).saveTo(sourceBase.resolve( config.getName() + "-image.tar.gz"))));
            return result.getDigest().getHash();
        }
    }

    private List<Path> mapToPaths(Dependency[] deps) {
        return Arrays.stream(deps).map(d ->
                {
                    Path given = Paths.get("." + d.getLocation().getPath());
                    Path result = sourceBase.resolve(given);
                    return result;
                }
        ).collect(Collectors.toList());
    }

    public void deployApplication(String imageId) throws IOException, ApiException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        AppsV1Api appsApi = new AppsV1Api();

        // create:
        V1Deployment deployment = createDeployment(imageId);
        LOG.info(Yaml.dump(deployment));
        Optional<V1Deployment> oldDeployment = existingDeployment(appsApi);
        if (oldDeployment.isPresent()) {
            LOG.info("Replace existing configuration:" + oldDeployment);
            appsApi.replaceNamespacedDeployment(config.getName(),config.getNamespace(),deployment,null,null,null);
        }else {
            LOG.info("Fresh deployment: " + deployment.getMetadata().getName());
            V1Deployment result = appsApi.createNamespacedDeployment(config.getNamespace(), deployment, null, null, null);
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
        V1Probe readinessProbe = new V1Probe();
        V1HTTPGetAction action = new V1HTTPGetAction();
        action.setPath("/foo");
        action.setPort(new IntOrString(8080));
        readinessProbe.setInitialDelaySeconds(5);
        readinessProbe.setHttpGet(action);
        container.setReadinessProbe(readinessProbe);

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
}
