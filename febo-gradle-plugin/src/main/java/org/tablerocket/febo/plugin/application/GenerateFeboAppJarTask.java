package org.tablerocket.febo.plugin.application;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.TaskAction;
import org.tablerocket.febo.api.FeboApplicationExtension;
import org.tablerocket.febo.api.TargetPlatformSpec;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GenerateFeboAppJarTask extends DefaultTask {

    @TaskAction
    public void exec() throws IOException, URISyntaxException {
        FeboApplicationExtension feboExtension = getProject().getExtensions().getByType(FeboApplicationExtension.class);
        ImageBuilder imageBuilder = new ImageBuilder(getProject().getName(),feboExtension);

        File output = new File(getProject().getBuildDir(), "libs/" + getProject().getName() + "-runner.jar");

        Configuration buildscriptConf = getProject().getBuildscript().getConfigurations().getByName("classpath");

        Optional<ResolvedArtifact> launcher = buildscriptConf.getResolvedConfiguration().getResolvedArtifacts().stream().filter(art -> "febo-osgi-launcher".equals(art.getName())).findAny();
        if (!launcher.isPresent()) throw new RuntimeException("Launcher artifact is not present in dependency set.");

        List<URI> artifacts = new ArrayList<>();
        Configuration projectConf = getProject().getConfigurations().getByName("compileClasspath");
        for (ResolvedArtifact artifact : projectConf.getResolvedConfiguration().getResolvedArtifacts()) {
            if (artifact.getFile().isFile()) {
                artifacts.add(artifact.getFile().toURI());
            }
        }
        URI itself = new File(getProject().getBuildDir().getAbsolutePath() + "/classes/java/main").toURI();
        artifacts.add(itself);
        TargetPlatformSpec spec = imageBuilder.buildRunnerJar(output,launcher.get().getFile().toURI(),artifacts);

        getLogger().info("Written a jar file to " + output.getAbsolutePath());
        if (feboExtension.isDeployImage()) {
            String hash = new ImageBuilder(getProject().getName(),feboExtension).containerize(output);
            getLogger().info("Written Image " + hash + " with name " + getProject().getName());
            getLogger().info("Image Repo ist " + feboExtension.getRepository());
        }else {
            getLogger().info("Skipped creating and deploying for " + getProject().getName());
        }
    }
}
