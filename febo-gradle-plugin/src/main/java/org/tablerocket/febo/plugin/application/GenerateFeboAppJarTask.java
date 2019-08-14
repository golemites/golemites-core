package org.tablerocket.febo.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import okio.Okio;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.TaskAction;
import org.tablerocket.febo.api.DelayedBuilder;
import org.tablerocket.febo.api.Dependency;
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
import java.util.ArrayList;
import java.util.Arrays;
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

import static org.tablerocket.febo.repository.ClasspathRepositoryStore.BLOB_FILENAME;

public class GenerateFeboAppJarTask extends DefaultTask {

    @TaskAction
    public void exec() throws IOException, URISyntaxException {
        File output = new File(getProject().getBuildDir(), "libs/" + getProject().getName() + "-runner.jar");
        TargetPlatformSpec spec = buildRunnerJar(output);
        getLogger().info("Written a jar file to " + output.getAbsolutePath());
        String hash = new Containerize(getProject().getName()).containerize(output);
        getLogger().info("Written Image " + hash + " with name "+ getProject().getName());
    }

    private TargetPlatformSpec buildRunnerJar(File output) throws IOException, URISyntaxException {
        Configuration buildscriptConf = getProject().getBuildscript().getConfigurations().getByName("classpath");
        Optional<ResolvedArtifact> launcher = buildscriptConf.getResolvedConfiguration().getResolvedArtifacts().stream().filter(art -> "febo-osgi-launcher".equals(art.getName())).findAny();
        Set<String> ignore = new HashSet<>(Arrays.asList("META-INF/DEPENDENCIES", "META-INF/MANIFEST.MF", "META-INF/LICENSE", "META-INF/NOTICE"));
        Configuration projectConf = getProject().getConfigurations().getByName("compileClasspath");

        if (launcher.isPresent()) {
            getLogger().info("Found launcher in " + launcher.get().getFile());
            output.getParentFile().mkdirs();
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Launcher.class.getName());
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(output), manifest)) {
                // Begin writing jos
                writeLauncherDeps(launcher.get(), ignore, jos);
                // Add Platform Jars
                TargetPlatformSpec platformSpec = writePlatformJars(projectConf, jos);
                // Add Autobundle Jars
                calculateAutobundles(projectConf, jos, platformSpec);
                // Calculate new platform blob file
                writeBlob(platformSpec, jos);
                return platformSpec;
            }
        } else {
            throw new RuntimeException("No launcher artifact found to be used in fatjar.");
        }
    }

    private void writeBlob(TargetPlatformSpec targetPlatformSpec, JarOutputStream jos) throws IOException {
        jos.putNextEntry(new JarEntry(BLOB_FILENAME));
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(jos, targetPlatformSpec);
    }

    private void calculateAutobundles(Configuration projectConf, JarOutputStream jos, TargetPlatformSpec platformBlob) throws IOException, URISyntaxException {
        AutoBundleSupport autobundle = new AutoBundleSupport();
        List<URL> potentialAutobundles = new ArrayList<>();
        for (ResolvedArtifact art : projectConf.getResolvedConfiguration().getResolvedArtifacts()) {
            getLogger().info("Found Artifact for autobundle: " + art.getFile().getAbsolutePath());
            potentialAutobundles.add(art.getFile().toURI().toURL());
        }
        // add itself, too!
        URL itself = new File(getProject().getBuildDir().getAbsolutePath() + "/classes/java/main").toURI().toURL();
        potentialAutobundles.add(itself);

        Set<DelayedBuilder<Dependency>> result = autobundle.discover(potentialAutobundles);
        List<Dependency> deps = new ArrayList<>(Arrays.asList(platformBlob.getDependencies()));
        for (DelayedBuilder<Dependency> auto : result) {
            // add it
            Dependency bundle = auto.build();
            getLogger().info("Writing Autobundle to Fatjar: " + bundle);
            String name = "APPLICATION/" + bundle.getIdentity() + ".jar";
            JarEntry targetName = new JarEntry(name);
            addAll(bundle.getLocation(), targetName, jos);
            deps.add(bundle);
            bundle.setLocation(new URI(name));
            deps.add(bundle);
        }
        platformBlob.setDependencies(deps.toArray(new Dependency[0]));
    }

    private TargetPlatformSpec writePlatformJars(Configuration c, JarOutputStream jos) throws IOException, URISyntaxException {
        boolean foundBlob = false;
        List<Dependency> deps = new ArrayList<>();
        for (ResolvedArtifact artifact : c.getResolvedConfiguration().getResolvedArtifacts()) {
            // here we first need to discover and resolve the existing blob file:
            getLogger().info("++ Project (in " + c.getName() + ") Found artifact: " + artifact.getFile());
            // find blobs:
            if (artifact.getFile().isFile()) {
                try (JarInputStream jis = new JarInputStream(new FileInputStream(artifact.getFile()))) {
                    JarEntry entry = null;
                    while ((entry = jis.getNextJarEntry()) != null) {
                        getLogger().info("Checking " + entry.getName() + " ---- ");

                        if (BLOB_FILENAME.equals(entry.getName())) {
                            ClasspathRepositoryStore repo = new ClasspathRepositoryStore(Okio.buffer(Okio.source(jis)).readByteArray());
                            for (Dependency dep : repo.platform().getDependencies()) {
                                String name = "PLATFORM/" + dep.getIdentity() + ".jar";
                                JarEntry targetName = new JarEntry(name);
                                addAll(dep.getLocation(), targetName, jos);
                                dep.setLocation(new URI(name));
                                deps.add(dep);
                                getLogger().info("Writing Platform to Fatjar: " + dep);

                            }
                            foundBlob = true;
                        }
                        getLogger().debug("Writing entry: " + entry.getName());
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

    private void writeLauncherDeps(ResolvedArtifact launcher, Set<String> ignore, JarOutputStream jos) throws IOException {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(launcher.getFile()))) {
            JarEntry entry = null;
            while ((entry = jis.getNextJarEntry()) != null) {
                getLogger().debug("Writing entry: " + entry.getName());
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
                        getLogger().debug("Duplicate entry " + ze.getMessage());
                    }

                }
            }
        }
    }

    private void printResolvedBuildscript() {
        for (Configuration c : getProject().getBuildscript().getConfigurations()) {
            getLogger().info("found buildscript configuration: " + c.getName());
            Set<File> resolved = c.resolve();
            Set<ResolvedArtifact> resolvedArtifacts = c.getResolvedConfiguration().getResolvedArtifacts();
            for (ResolvedArtifact artifact : resolvedArtifacts) {
                getLogger().info("++ Buildscript " + c.getName() + ") Found artifact: " + artifact.getFile());
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
