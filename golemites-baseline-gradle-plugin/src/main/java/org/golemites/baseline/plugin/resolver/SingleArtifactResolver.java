package org.golemites.baseline.plugin.resolver;

import java.io.File;

@FunctionalInterface
public interface SingleArtifactResolver {
    File resolve(ArtifactDescriptor descriptor);
}
