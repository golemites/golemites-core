package org.golemites.baseline.plugin.resolver;

import lombok.Data;

@Data
public class FeaturedBundle {

    boolean isDependency = false;

    boolean isConditional = false;

    Feature parentFeature;

    String url;
}
