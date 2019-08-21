package org.golemites.api;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class TargetPlatformSpec
{
    @Getter
    @Setter
    private Dependency[] dependencies;

    @Getter
    @Setter
    private String imageID;
}
