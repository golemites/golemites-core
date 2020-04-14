package org.golemites.launcher.resolver;

import lombok.Data;

@Data
public class FeaturedBundle {

    boolean isDependency = false;

    boolean isConditional = false;

    Feature parentFeature;

    String url;
}
