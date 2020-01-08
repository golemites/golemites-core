package org.golemites.plugin.application;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.RegistryException;
import io.kubernetes.client.ApiException;
import org.golemites.api.GolemitesApplicationExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class DeployTask extends DefaultTask {

    @TaskAction
    public void exec() throws IOException, ApiException, InterruptedException, ExecutionException, RegistryException, CacheDirectoryCreationException, InvalidImageReferenceException {
        GolemitesApplicationExtension extension = getProject().getExtensions().getByType(GolemitesApplicationExtension.class);
        Path base = getProject().getBuildDir().toPath().resolve("golemites-build");
        Optional<ResolvedArtifact> launcher = locateLauncher(getProject());

        CloudDeployer deployer = new CloudDeployer(launcher.get().getFile().toPath(),base,extension);
        String id = deployer.createImage(Paths.get("/"));
        deployer.deployApplication("@sha256:" + id);
    }

    static Optional<ResolvedArtifact> locateLauncher(Project project) {
        Configuration buildscriptConf = project.getBuildscript().getConfigurations().getByName("classpath");
        Optional<ResolvedArtifact> launcher = buildscriptConf.getResolvedConfiguration().getResolvedArtifacts().stream().filter(art -> "golemites-osgi-launcher".equals(art.getName())).findAny();
        if (!launcher.isPresent()) throw new RuntimeException("Launcher artifact is not present in dependency set.");
        return launcher;
    }
}
