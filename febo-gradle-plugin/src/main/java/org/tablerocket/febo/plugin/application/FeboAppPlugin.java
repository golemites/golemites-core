package org.tablerocket.febo.plugin.application;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.tablerocket.febo.api.FeboApplicationExtension;

public class FeboAppPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().log(LogLevel.INFO,"Creating FEBO Fatjar.");
        FeboApplicationExtension ext = project.getExtensions().create("feboApplication", FeboApplicationExtension.class);
        project.getLogger().info("Created extension " + ext + " with " + ext.getRepository());
        Task generateFeboJar = project.getTasks().create( "generateFeboJar", GenerateFeboAppJarTask.class);
        // make compileJava depend on generating sourcecode
        project.getTasks().getByName("jar").dependsOn(generateFeboJar);
    }
}
