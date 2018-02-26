package org.tablerocket.febo.plugin.resolver;

import lombok.Data;

@Data
public class Feature {
    ArtifactDescriptor featureRepository;

    String name;

    String version;

    boolean isDependency = false;

}
