package org.tablerocket.febo.plugin.resolver;

import lombok.Data;

@Data
public class FeatureDependency {

    private String name;

    private String version;

    private boolean isPrerequisite = true;

    private boolean isDependency = true;
}
