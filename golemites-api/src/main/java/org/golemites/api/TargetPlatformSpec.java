package org.golemites.api;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.nio.file.Path;

@ToString
@EqualsAndHashCode
public class TargetPlatformSpec
{
    private static final String CONFIGURATION = "CONFIGURATION";
    private static final String PLATFORM = "PLATFORM";
    private static final String APPLICATION = "APPLICATION";

    @Getter
    @Setter
    private Dependency[] dependencies;

    @Getter
    @Setter
    private Dependency[] application;

    @Getter
    @Setter
    private String imageID;

    @Getter
    @Setter
    private String buildTimeUTC;

    public static Path platformPath(Path base) {
        return base.resolve(PLATFORM);
    }

    public static Path applicationPath(Path base) {
        return base.resolve(APPLICATION);
    }

    public static Path configuration(Path base) {
        return base.resolve(CONFIGURATION);
    }
}
