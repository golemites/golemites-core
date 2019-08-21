package org.golemites.baseline.plugin

import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.WorkResult
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

    private static final class ReproducibleOrderingCopyAction implements CopyAction {

        private final CopyAction delegate;

        private ReproducibleOrderingCopyAction(CopyAction delegate) {
            this.delegate = delegate;
        }

        @Override
        WorkResult execute(CopyActionProcessingStream stream) {
            return this.delegate.execute({ action ->
                Map<RelativePath, FileCopyDetailsInternal> detailsByPath = new TreeMap<>();
                stream.process({ details ->
                    detailsByPath.put(details.getRelativePath(),
                            details)
                });
                detailsByPath.values().forEach(action.&processFile);
            });
        }

    }
}
