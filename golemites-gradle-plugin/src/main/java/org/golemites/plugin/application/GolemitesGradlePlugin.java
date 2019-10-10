package org.golemites.plugin.application;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.LogLevel;
import org.golemites.api.GolemitesApplicationExtension;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.Set;

public class GolemitesGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        Configuration config = project.getConfigurations().maybeCreate("repository");
        config.setTransitive(false);

        project.getLogger().log(LogLevel.INFO,"Creating Golemites Gestalt");
        GolemitesApplicationExtension ext = project.getExtensions().create("golemites", GolemitesApplicationExtension.class);
        project.getLogger().info("Created extension " + ext + " with " + ext.getRepository());

        File generatedResourcesDir = new File(project.getBuildDir(), "generated/resources");
        generatedResourcesDir.mkdirs();
        File output = new File(generatedResourcesDir, "febo-blobs.json");
        Task makeGestalt = project.getTasks().create( "gestalt", MakeGestaltTask.class, output);

        Task installTask = project.getTasks().create( "install", InstallTask.class);
        Task deployTask = project.getTasks().create( "deploy", DeployTask.class);

        // make compileJava depend on generating sourcecode
        deployTask.dependsOn(installTask);

        SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
        SourceSet versionSet = sourceSets.getByName("main");
        Set<File> directories = versionSet.getResources().getSrcDirs();
        directories.add(generatedResourcesDir);
        project.getLogger().warn("Source directories now: " + directories);
        versionSet.getResources().setSrcDirs(directories);

        //project.getTasks().getByName("build");
        project.getRootProject().getAllprojects().forEach(p -> {
            if (p.getTasks().findByName("jar") != null) {
                if (!p.equals(project)) {
                    project.getLogger().info(makeGestalt.getName() + " depends on " + p.getName());
                    makeGestalt.dependsOn(p.getTasks().getByName("jar"));
                }else {
                    project.getLogger().info("!! " + makeGestalt.getName() + " runs before jar on " + p.getName());
                    p.getTasks().getByName("jar").dependsOn(makeGestalt);
                    makeGestalt.finalizedBy(p.getTasks().getByName("jar"));
                    //p.getTasks().getByName("build").dependsOn(makeGestalt);
                }
            }
        });
        //makeGestalt.dependsOn(project.getTasks().getByName("jar"));
    }
}
