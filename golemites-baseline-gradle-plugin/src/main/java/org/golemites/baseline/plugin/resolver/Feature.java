package org.golemites.baseline.plugin.resolver;

import lombok.Data;

@Data
class Feature {
    ArtifactDescriptor featureRepository;

    String name;

    String version;

    boolean isDependency = false;

}
