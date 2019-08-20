package org.tablerocket.febo.plugin

import org.gradle.api.Project

class FeboExtension {

    String packageName

    boolean buildBaseline = true

    boolean buildRepository = true

    FeboExtension(Project project) {

    }
}