package org.tablerocket.febo.plugin.application;

import okio.Okio;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.TaskAction;
import org.tablerocket.febo.api.DelayedBuilder;
import org.tablerocket.febo.api.Dependency;
import org.tablerocket.febo.autobundle.AutoBundleSupport;
import org.tablerocket.febo.launcher.Launcher;
import org.tablerocket.febo.repository.ClasspathRepositoryStore;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipException;

import static org.tablerocket.febo.repository.ClasspathRepositoryStore.BLOB_FILENAME;

public class GenerateFeboAppJarTask extends DefaultTask {

    @TaskAction
    public void exec() throws IOException {
        File target = getProject().getBuildDir();
        // create a simple jar, just for testing the principle
        File output = new File(target,"libs/" + getProject().getName() + "-runner.jar");
        output.getParentFile().mkdirs();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Launcher.class.getName());

        // Find content of launcher and some other artifacts and feed it into jar
        // 1) launcher and all of its dependencies!


        Configuration buildscriptConf = getProject().getBuildscript().getConfigurations().getByName("classpath");
        Optional<ResolvedArtifact> launcher = buildscriptConf.getResolvedConfiguration().getResolvedArtifacts().stream().filter(art -> "febo-osgi-launcher".equals(art.getName())).findAny();
        Set<String> ignore = new HashSet<>(Arrays.asList("META-INF/DEPENDENCIES","META-INF/MANIFEST.MF","META-INF/LICENSE","META-INF/NOTICE"));
        Configuration projectConf = getProject().getConfigurations().getByName("compileClasspath");

        if (launcher.isPresent()) {
            getLogger().info("Found launcher in " + launcher.get().getFile());

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(output), manifest)) {
                // Begin writing jos
                writeLauncherDeps(launcher, ignore, jos);
                // Add Platform Jars
                Properties platformBlob = new Properties();
                writePlatformJars(projectConf, platformBlob, jos);
                // Add Autobundle Jars
                calculateAutobundles(projectConf, jos, platformBlob);
                // Calculate new platform blob file
                writeBlob(platformBlob,jos);
            }
        }else {
            throw new RuntimeException("No launcher artifact found to be used in fatjar.");
        }
        getLogger().info("Written a jar file to " + output.getAbsolutePath());
    }

    private void writeBlob(Properties platformBlob, JarOutputStream jos) throws IOException {
        jos.putNextEntry(new JarEntry(BLOB_FILENAME));
        platformBlob.store(jos,"# Created by Gradle Febo Plugin.");
    }

    private void calculateAutobundles(Configuration projectConf, JarOutputStream jos, Properties platformBlob) throws IOException {
        AutoBundleSupport autobundle = new AutoBundleSupport();
        List<URL> potentialAutobundles = new ArrayList<>();
        for (ResolvedArtifact art : projectConf.getResolvedConfiguration().getResolvedArtifacts()) {
            getLogger().info("Found Artifact for autobundle: " + art.getFile().getAbsolutePath());
            potentialAutobundles.add(art.getFile().toURI().toURL());
        }
        // add itself, too!
        URL itself = new File(getProject().getBuildDir().getAbsolutePath() + "/classes/java/main").toURI().toURL();
        potentialAutobundles.add(itself);

        Set<DelayedBuilder<Dependency>> result = autobundle.scan(potentialAutobundles);
        for (DelayedBuilder<Dependency> auto : result) {
            // add it
            Dependency bundle = auto.build();
            getLogger().info("Writing Autobundle to Fatjar: " + bundle.identity() + " --> " + bundle.location().toASCIIString());
            String name = "APPLICATION/" + bundle.identity() + ".jar";
            JarEntry targetName = new JarEntry(name);
            addAll(bundle.location(),targetName,jos);
            platformBlob.put(bundle.identity(),name);
        }
    }

    private Properties writePlatformJars(Configuration c, Properties targetPlatformBlob, JarOutputStream jos) throws IOException {
        boolean foundBlob = false;
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
                            // found!
                            Properties p = new Properties();
                            p.load(jis);
                            ClasspathRepositoryStore repo = new ClasspathRepositoryStore(p);
                            for (Dependency dep : repo.platform()) {
                                getLogger().info("Writing Platform to Fatjar: " + dep.identity() + " --> " + dep.location().toASCIIString());
                                String name = "PLATFORM/" + dep.identity() + ".jar";
                                JarEntry targetName = new JarEntry(name);
                                addAll(dep.location(),targetName,jos);
                                targetPlatformBlob.put(dep.identity(),name);
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
        return targetPlatformBlob;
    }

    private void writeLauncherDeps(Optional<ResolvedArtifact> launcher, Set<String> ignore, JarOutputStream jos) throws IOException {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(launcher.get().getFile()))) {
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
                        }catch (ZipException ze) {
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

    private void addAll(URI source, JarEntry entry, JarOutputStream target) throws IOException
    {
        BufferedInputStream in = null;
        try
        {
            target.putNextEntry(entry);
            in = new BufferedInputStream(source.toURL().openStream());

            byte[] buffer = new byte[1024];
            while (true)
            {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        }
        finally
        {
            if (in != null)
                in.close();
        }
    }

}
