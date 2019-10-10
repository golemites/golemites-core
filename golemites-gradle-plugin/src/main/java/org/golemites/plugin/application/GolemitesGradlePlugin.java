package org.golemites.plugin.application;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.LogLevel;
import org.golemites.api.GolemitesApplicationExtension;

public class GolemitesGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        Configuration config = project.getConfigurations().maybeCreate("repository");
        config.setTransitive(false);

        project.getLogger().log(LogLevel.INFO,"Creating Golemites Gestalt");
        GolemitesApplicationExtension ext = project.getExtensions().create("golemites", GolemitesApplicationExtension.class);
        project.getLogger().info("Created extension " + ext + " with " + ext.getRepository());

        Task makeGestalt = project.getTasks().create( "gestalt", MakeGestaltTask.class);

        Task installTask = project.getTasks().create( "install", InstallTask.class);
        Task deployTask = project.getTasks().create( "deploy", DeployTask.class);

        // make compileJava depend on generating sourcecode
        deployTask.dependsOn(installTask);

        //project.getTasks().getByName("build");
        project.getRootProject().getAllprojects().forEach(p -> {
            if (p.getTasks().findByName("jar") != null) {
                project.getLogger().info(makeGestalt.getName() + " depends on " + p.getName());
                makeGestalt.dependsOn(p.getTasks().getByName("jar"));
            }
        });
        //makeGestalt.dependsOn(project.getTasks().getByName("jar"));
    }
}
