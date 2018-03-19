package org.tablerocket.febo.plugin

import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

@CacheableTask
class FeboJarTask extends Jar {

    @Override
    protected CopyAction createCopyAction() {
        CopyAction copyAction = new BootZipCopyAction(jar.getArchivePath(),
                jar.isPreserveFileTimestamps(), isUsingDefaultLoader(jar),
                this.requiresUnpack.getAsSpec(), this.exclusions.getAsExcludeSpec(),
                this.launchScript, this.compressionResolver, jar.getMetadataCharset());
        if (!jar.isReproducibleFileOrder()) {
            return copyAction;
        }
        return new ReproducibleOrderingCopyAction(copyAction);
    }
}
