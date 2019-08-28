package org.golemites.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.TargetPlatformSpec;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class InstallTask extends DefaultTask {

    @TaskAction
    public void exec() throws IOException {
        GolemitesApplicationExtension extension = getProject().getExtensions().getByType(GolemitesApplicationExtension.class);
        Path base = Files.createDirectories(getProject().getBuildDir().toPath().resolve("golemites-build"));
        Configuration dependencies = getProject().getConfigurations().getByName("runtimeClasspath");

        //debug();
        List<URI> artifacts = new ArrayList<>();
        for (ResolvedArtifact artifact : dependencies.getResolvedConfiguration().getResolvedArtifacts()) {
            getLogger().warn(" + Include " + artifact.getFile().toURI());
            artifacts.add(artifact.getFile().toURI());
        }
        // TODO: get the path from gradle instead!
        URI itself = new File(getProject().getBuildDir().getAbsolutePath() + "/libs/" + getProject().getName() + "-" + getProject().getVersion() + ".jar").toURI();
        getLogger().warn(" + Include (self) " + itself + " Exists: " + Files.exists(Paths.get(itself)));


        artifacts.add(itself);
        artifacts.forEach( t -> getLogger().info(" ------> " + t.toASCIIString()));
        ImageBuilder imageBuilder = new ImageBuilder(base);
        TargetPlatformSpec input = imageBuilder.findSpec(artifacts);
        TargetPlatformSpec result = imageBuilder.prepare(input,artifacts);
        getLogger().info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }

    private void debug() {
        for (Configuration c : getProject().getConfigurations()) {
            getLogger().warn("Examine " + c);
            try {
                for (ResolvedArtifact thing : c.getResolvedConfiguration().getResolvedArtifacts()) {
                    getLogger().warn(" + Examine (" + c.getName() + ") " + thing.getFile().toURI());
                }
            }catch(Exception e) {
                getLogger().warn("Ignoring: " + e.getMessage() + " for " + c);
            }
        }
    }
}
