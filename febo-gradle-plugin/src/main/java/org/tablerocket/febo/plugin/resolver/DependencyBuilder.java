package org.tablerocket.febo.plugin.resolver;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyBuilder {

    @Getter
    private final FeatureDependency rootFeature;

    private Map<String,BundleDependency> bundles = new HashMap<>();
    private Map<String,FeatureDependency> featureDependencies = new HashMap<>();

    public DependencyBuilder(FeatureDependency root) {
        this.rootFeature = root;
    }

    public DependencyBuilder add(BundleDependency bundleDependency) {
        bundles.put(bundleDependency.getUrl(),bundleDependency);
        return this;
    }

    public DependencyBuilder add(FeatureDependency featureDependency) {
        featureDependencies.put(featureDependency.getName(),featureDependency);
        return this;
    }

    public List<BundleDependency> build() {
        return Arrays.asList( bundles.values().toArray(new BundleDependency[bundles.values().size()]) );
    }

    public boolean containsFeature( FeatureDependency dep )
    {
        return (featureDependencies.containsKey( dep ));
    }

    public int getBundleCount()
    {
        return bundles.size();
    }
}
