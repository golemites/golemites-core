package org.golemites.launcher.resolver;

import lombok.Data;

@Data
public class BundleDependency {
    private String url;
    private int startLevel;
    private String version;
    private FeatureDependency sourceFeature;
}
