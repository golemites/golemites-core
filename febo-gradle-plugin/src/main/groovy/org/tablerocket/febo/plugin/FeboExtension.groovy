package org.tablerocket.febo.plugin

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property

class FeboExtension {
     String packageName

    FeboExtension(Project project) {
        project.getLogger().log(LogLevel.WARN,"Loading Extension..")
        //message = project.objects.property(String)
        //message.set('Hello from GreetingPlugin')
        //outputFiles = project.files()
    }
}
