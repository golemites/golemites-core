package org.golemites.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import okio.Okio;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.TargetPlatformSpec;
import org.golemites.repository.ClasspathRepositoryStore;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class InstallTask extends DefaultTask {

    private final File outputFolder;
    private File input;

    @Inject
    public InstallTask(File input, File outputFolder) {
        this.input = input;
        this.outputFolder = outputFolder;
    }

    @InputFile
    public File getInput() {
        return input;
    }

    @OutputDirectory
    public File getOutput() {
        return outputFolder;
    }

    @TaskAction
    public void exec() throws IOException {
        GolemitesApplicationExtension extension = getProject().getExtensions().getByType(GolemitesApplicationExtension.class);
        Path base = outputFolder.toPath();

        ImageBuilder imageBuilder = new ImageBuilder(base);

        // spec is built by GestaltTask.
        TargetPlatformSpec spec  = new ClasspathRepositoryStore(Okio.buffer(Okio.source(input)).readByteArray()).platform();
        TargetPlatformSpec result = imageBuilder.prepare(spec);
        getLogger().info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
        Optional<ResolvedArtifact> resolvedArtifact = DeployTask.locateLauncher(getProject());
        Path output = base.resolve(extension.getName() + "-standalone.jar");
        imageBuilder.assembleJar(output,resolvedArtifact.get().getFile().toURI(),result);
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
