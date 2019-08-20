/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed require this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  require the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/* Based on a forked buildscript from Apache Polygene. */
package org.tablerocket.febo.plugin

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.GradleVersion

@CompileStatic
class FeboPlugin implements Plugin<Project> {

    void apply(Project project) {
        verifyGradleVersion()
        project.getPlugins().apply(JavaLibraryPlugin.class)
        def extension = project.extensions.create("febo",FeboExtension, project)

        Task makeVersionClassTask = project.tasks.create( 'generateStaticApi', GenerateStaticApiTask.class)
        //Task feboJar = project.tasks.create( 'feboJar', FeboJarTask.class)

        project.getConfigurations().maybeCreate("repository")
        project.getConfigurations().maybeCreate("baseline")

        // make sure that our generated src folder is part of the main source set.
        File generatedSrcDir = new File(project.buildDir, 'generated/java')
        File generatedResourcesDir = new File(project.buildDir, 'generated/resources')

        def sourceSets = project.convention.getPlugin(JavaPluginConvention).sourceSets
        def versionSet = sourceSets.getByName("main") { SourceSet sourceSet ->
            sourceSet.java { SourceDirectorySet dirSet ->
                dirSet.srcDir generatedSrcDir
            }
            sourceSet.resources { SourceDirectorySet dirSet ->
                dirSet.srcDir generatedResourcesDir
            }
        }

        // all source as input? questionable.

        def tmpGroup = project.name
        tmpGroup = tmpGroup.replace('-', '_')
        def outFilename = "java/" + tmpGroup.replace('.', '/') + "/BuildVersion.java"
        def outFile = new File(generatedSrcDir, outFilename)

        makeVersionClassTask.getInputs().files(sourceSets.getByName('main').allSource)

        makeVersionClassTask.getOutputs().file(outFile)
        if (project.getBuildFile() != null && project.getBuildFile().exists()) {
            makeVersionClassTask.getInputs().files(project.getBuildFile())
        }

        // make compileJava depend on generating sourcecode
        project.getTasks().getByName('compileJava').dependsOn('generateStaticApi')

        project.getTasks().getByName('jar') { Jar task ->
            task.from versionSet.output
        }

        // finally: configure task(s):
        project.afterEvaluate {
            configure(project, makeVersionClassTask, extension,generatedSrcDir,generatedResourcesDir)
        }
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current() < GradleVersion.version("5.0")) {
            throw new GradleException("Febo plugin requires Gradle 5.0 or later."
                    + " The current version is " + GradleVersion.current());
        }
    }

    private void configure(Project project, GenerateStaticApiTask task, FeboExtension extension, File generatedSrcDir,File generatedResourcesDir) {
        // configure task:
        task.packageName = extension.packageName
        task.outputLocation = generatedSrcDir
        task.generatedResourcesDir = generatedResourcesDir
    }
}
