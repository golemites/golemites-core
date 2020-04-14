package org.golemites.launcher.resolver;

import lombok.Data;

@Data
class Feature {
    ArtifactDescriptor featureRepository;

    String name;

    String version;

    boolean isDependency = false;

}
