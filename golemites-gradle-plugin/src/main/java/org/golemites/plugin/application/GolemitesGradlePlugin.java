package org.golemites.plugin.application;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.golemites.api.GolemitesApplicationExtension;

public class GolemitesGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().log(LogLevel.INFO,"Creating FEBO Fatjar.");
        GolemitesApplicationExtension ext = project.getExtensions().create("feboApplication", GolemitesApplicationExtension.class);
        project.getLogger().info("Created extension " + ext + " with " + ext.getRepository());
        Task generateFeboJar = project.getTasks().create( "generateFeboJar", GenerateGolemiteImageTask.class);
        // make compileJava depend on generating sourcecode
        project.getTasks().getByName("jar").dependsOn(generateFeboJar);
    }
}
