package org.tablerocket.febo.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.tools.jib.api.*;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tablerocket.febo.api.DelayedBuilder;
import org.tablerocket.febo.api.Dependency;
import org.tablerocket.febo.api.FeboApplicationExtension;
import org.tablerocket.febo.api.TargetPlatformSpec;
import org.tablerocket.febo.autobundle.AutoBundleSupport;
import org.tablerocket.febo.launcher.Launcher;
import org.tablerocket.febo.repository.ClasspathRepositoryStore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import static org.tablerocket.febo.repository.ClasspathRepositoryStore.BLOB_FILENAME;

public class ImageBuilder {
    private final static Logger LOG = LoggerFactory.getLogger(ImageBuilder.class);

    public static final String BASE_IMAGE = "openjdk:8-jre-alpine";
    public static final String LATEST_VERSION = "latest";
    public static final String JAVA_PATH = "/usr/bin/java";

    private final String name;
    private final FeboApplicationExtension config;

    public ImageBuilder(String name,FeboApplicationExtension config) {
        this.name = name;
        this.config = config;
    }

    public String containerize(File output) throws IOException {
        // TODO: add dependencies into separate layers.
        try {
            ImageReference ref = ImageReference.parse(config.getRepository());
            JibContainer result = Jib.from(BASE_IMAGE)
                    .addLayer(Collections.singletonList(output.toPath()), AbsoluteUnixPath.get("/"))
                    .setEntrypoint(JAVA_PATH, "-jar", "/" + output.getName())
                    .setCreationTime(Instant.now())
                    .containerize(Containerizer.to(RegistryImage.named(ref)
                            .addCredentialRetriever(CredentialRetrieverFactory.forImage(ref).dockerConfig())));
            return result.getImageId().getHash();
        } catch (InterruptedException | RegistryException | CacheDirectoryCreationException | ExecutionException | InvalidImageReferenceException e) {
            // getLogger().error("Problem creating image",e);
            throw new RuntimeException(e);
        }
    }

    public TargetPlatformSpec buildRunnerJar(File output, URI launcher, List<URI> projectConf) throws IOException, URISyntaxException {
        Set<String> ignore = new HashSet<>(Arrays.asList("META-INF/DEPENDENCIES", "META-INF/MANIFEST.MF", "META-INF/LICENSE", "META-INF/NOTICE"));
        output.getParentFile().mkdirs();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Launcher.class.getName());
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(output), manifest)) {
            // Begin writing jos
            writeLauncherDeps(launcher, ignore, jos);
            // Add Platform Jars
            TargetPlatformSpec platformSpec = writePlatformJars(projectConf, jos);
            // Add Autobundle Jars
            calculateAutobundles(projectConf, jos, platformSpec);
            // Calculate new platform blob file
            writeBlob(platformSpec, jos);
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(platformSpec));
            return platformSpec;
        }
    }

    private void writeBlob(TargetPlatformSpec targetPlatformSpec, JarOutputStream jos) throws IOException {
        jos.putNextEntry(new JarEntry(BLOB_FILENAME));
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(jos, targetPlatformSpec);
    }

    private void calculateAutobundles(List<URI> projectConf, JarOutputStream jos, TargetPlatformSpec platformBlob) throws IOException, URISyntaxException {
        AutoBundleSupport autobundle = new AutoBundleSupport();
        List<URL> potentialAutobundles = new ArrayList<>();
        for (URI art : projectConf) {
            LOG.info("Found Artifact for autobundle: " + art.toASCIIString());
            potentialAutobundles.add(art.toURL());
        }
        // add itself, too!
        //potentialAutobundles.add(itselfUri.toURL());

        Set<DelayedBuilder<Dependency>> result = autobundle.discover(potentialAutobundles);
        List<Dependency> deps = new ArrayList<>(Arrays.asList(platformBlob.getDependencies()));
        for (DelayedBuilder<Dependency> auto : result) {
            // add it
            Dependency bundle = auto.build();
            LOG.info("Writing Autobundle to Fatjar: " + bundle);
            String name = "APPLICATION/" + bundle.getIdentity() + ".jar";
            JarEntry targetName = new JarEntry(name);
            addAll(bundle.getLocation(), targetName, jos);
            bundle.setLocation(new URI(name));
            deps.add(bundle);
        }
        platformBlob.setDependencies(deps.toArray(new Dependency[0]));
    }

    private TargetPlatformSpec writePlatformJars(List<URI> c, JarOutputStream jos) throws IOException, URISyntaxException {
        boolean foundBlob = false;
        List<Dependency> deps = new ArrayList<>();
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
                            ClasspathRepositoryStore repo = new ClasspathRepositoryStore(Okio.buffer(Okio.source(jis)).readByteArray());
                            for (Dependency dep : repo.platform().getDependencies()) {
                                String name = "PLATFORM/" + dep.getIdentity() + ".jar";
                                JarEntry targetName = new JarEntry(name);
                                addAll(dep.getLocation(), targetName, jos);
                                dep.setLocation(new URI(name));
                                deps.add(dep);
                                LOG.info("Writing Platform to Fatjar: " + dep);
                            }
                            foundBlob = true;
                        }
                        LOG.debug("Writing entry: " + entry.getName());
                    }
                }
            }
        }
        if (!foundBlob) {
            throw new RuntimeException("There was no platform found in configuration file.");
        }
        return asTargetPlatform(deps);
    }

    private TargetPlatformSpec asTargetPlatform(List<Dependency> deps) {
        TargetPlatformSpec platformSpec = new TargetPlatformSpec();
        platformSpec.setDependencies(deps.toArray(new Dependency[0]));
        return platformSpec;
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

    private void addAll(URI source, JarEntry entry, JarOutputStream target) throws IOException {
        BufferedInputStream in = null;
        try {
            target.putNextEntry(entry);
            in = new BufferedInputStream(source.toURL().openStream());

            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        } finally {
            if (in != null)
                in.close();
        }
    }
}
