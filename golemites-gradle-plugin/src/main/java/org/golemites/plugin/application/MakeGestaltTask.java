package org.golemites.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.golemites.api.Dependency;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.Metadata;
import org.golemites.api.TargetPlatformSpec;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.rebaze.integrity.tree.api.TreeSession;
import org.rebaze.integrity.tree.util.DefaultTreeSessionFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class MakeGestaltTask extends DefaultTask {

    private File output;

    @Inject
    public MakeGestaltTask(File output) {
        this.output = output;
    }

    @OutputFile
    public File getOutput() {
        return output;
    }

    @TaskAction
    public void exec() throws IOException {
        TreeSession session = new DefaultTreeSessionFactory().create();
        ObjectMapper mapper = new ObjectMapper();

        GolemitesApplicationExtension extension = getProject().getExtensions().getByType(GolemitesApplicationExtension.class);
        getLogger().info("Running " + getProject().getName() + " gestalt task");
        TargetPlatformSpec platform = new TargetPlatformSpec();
        List<Dependency> platformDeps = new LinkedList<>();

        Configuration repoConfig = getProject().getConfigurations().getByName("repository");
        for (ResolvedArtifact deps : repoConfig.getResolvedConfiguration().getResolvedArtifacts()) {
            getLogger().warn(" >> (from repository)" + deps.getFile().getName());

            Dependency dependency = Dependency.dependency(
                    session.createStreamTreeBuilder().add(deps.getFile()).seal().value().hash(),
                    deps.getFile().toURI(),
                    Metadata.metadata(
                            deps.getModuleVersion().getId().getGroup(),
                            deps.getName(),
                            deps.getModuleVersion().getId().getVersion(),
                            deps.getClassifier(),
                            deps.getType(),
                            readBsn(deps.getFile())
                    ));
            platformDeps.add(dependency);
        }
        // if the current project is a valid bundle, then we need to add this, too:
        // Get Hold of Jar:
        Configuration runtime = getProject().getConfigurations().getByName("runtimeClasspath");

        for (PublishArtifact deps : runtime.getAllArtifacts()) {
            getLogger().warn(" >> (from classpath) " + deps.getFile().getName());
            Dependency dependency = Dependency.dependency(
                    session.createStreamTreeBuilder().add(deps.getFile()).seal().value().hash(),
                    deps.getFile().toURI(),
                    Metadata.metadata(
                            getProject().getGroup().toString(),
                            deps.getName(),
                            getProject().getVersion().toString(),
                            deps.getClassifier(),
                            deps.getType(),
                            readBsn(deps.getFile())
                    ));
            platformDeps.add(dependency);
        }

        platform.setDependencies(platformDeps.toArray(new Dependency[0]));
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        platform.setBuildTimeUTC(dtf.format(Instant.now()));
        getLogger().info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(platform));
        mapper.writeValue(getOutput(),platform);
        getLogger().info("Output: " + getOutput().getAbsolutePath());
    }

    private String readBsn(File f) throws IOException {
        try (JarInputStream jin = new JarInputStream(new FileInputStream(f))) {
            Manifest manifest = jin.getManifest();
            return manifest.getMainAttributes().getValue("Bundle-SymbolicName");
        }
    }
}
