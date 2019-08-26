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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class InstallTask extends DefaultTask {

    @TaskAction
    public void exec() throws IOException {
        GolemitesApplicationExtension extension = getProject().getExtensions().getByType(GolemitesApplicationExtension.class);
        Path base = getProject().getBuildDir().toPath().resolve("golemites-build");
        Files.createDirectories(base);
        Configuration projectConf = getProject().getConfigurations().getByName("compileClasspath");

        List<URI> artifactsAll = new ArrayList<>();
        for (ResolvedArtifact artifact : projectConf.getResolvedConfiguration().getResolvedArtifacts()) {
            artifactsAll.add(artifact.getFile().toURI());
        }
        List<URI> artifacts = new ArrayList<>(new LinkedHashSet<>(artifactsAll));

        URI itself = new File(getProject().getBuildDir().getAbsolutePath() + "/classes/java/main").toURI();
        artifacts.add(itself);
        artifacts.forEach( t -> getLogger().info(" ------> " + t.toASCIIString()));
        ImageBuilder imageBuilder = new ImageBuilder(base);
        TargetPlatformSpec input = imageBuilder.findSpec(artifacts);
        TargetPlatformSpec result = imageBuilder.prepare(input,artifacts);
        getLogger().info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }
}
