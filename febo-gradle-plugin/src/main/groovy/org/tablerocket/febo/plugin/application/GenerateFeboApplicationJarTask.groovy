package org.tablerocket.febo.plugin.application

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GenerateFeboApplicationJarTask extends DefaultTask {

    @TaskAction
    public void execute() {
        project.buildscript.dependencies.each {

        }

    }
}
