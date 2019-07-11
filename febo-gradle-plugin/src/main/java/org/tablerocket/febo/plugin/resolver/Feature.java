package org.tablerocket.febo.plugin.resolver;

import lombok.Data;

@Data
class Feature {
    ArtifactDescriptor featureRepository;

    String name;

    String version;

    boolean isDependency = false;

}
