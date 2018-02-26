package org.tablerocket.febo.plugin.resolver;

import java.io.File;

@FunctionalInterface
public interface SingleArtifactResolver {
    File resolve( ArtifactDescriptor descriptor );
}
