package org.golemites.launcher.resolver;

import java.io.File;

@FunctionalInterface
public interface SingleArtifactResolver {
    File resolve(ArtifactDescriptor descriptor);
}
