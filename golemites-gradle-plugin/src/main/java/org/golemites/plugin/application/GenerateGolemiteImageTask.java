package org.golemites.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.TaskAction;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.TargetPlatformSpec;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public class GenerateGolemiteImageTask extends DefaultTask {

    @TaskAction
    public void exec() throws IOException, URISyntaxException {
        GolemitesApplicationExtension extension = getProject().getExtensions().getByType(GolemitesApplicationExtension.class);
        ImageBuilder imageBuilder = new ImageBuilder(getProject().getName(),extension);

        Configuration buildscriptConf = getProject().getBuildscript().getConfigurations().getByName("classpath");

        Optional<ResolvedArtifact> launcher = buildscriptConf.getResolvedConfiguration().getResolvedArtifacts().stream().filter(art -> "golemites-osgi-launcher".equals(art.getName())).findAny();
        if (!launcher.isPresent()) throw new RuntimeException("Launcher artifact is not present in dependency set.");

        Configuration projectConf = getProject().getConfigurations().getByName("compileClasspath");

        List<URI> artifactsAll = new ArrayList<>();
        for (ResolvedArtifact artifact : projectConf.getResolvedConfiguration().getResolvedArtifacts()) {
            artifactsAll.add(artifact.getFile().toURI());
        }
        List<URI> artifacts = new ArrayList<>(new LinkedHashSet<>(artifactsAll));

        URI itself = new File(getProject().getBuildDir().getAbsolutePath() + "/classes/java/main").toURI();
        artifacts.add(itself);
        artifacts.forEach( t -> getLogger().info(" ------> " + t.toASCIIString()));
        TargetPlatformSpec input = imageBuilder.findSpec(artifacts);
        File output = new File(getProject().getBuildDir(), "libs/" + getProject().getName() + "-runner.jar");

        TargetPlatformSpec result = imageBuilder.build(output,launcher.get().getFile().toURI(),input,artifacts);

        getLogger().info("Written a jar file to " + output.getAbsolutePath());
        getLogger().info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }
}
