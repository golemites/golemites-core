package org.tablerocket.febo.api;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

@ToString
@EqualsAndHashCode
public class TargetPlatformSpec
{
    @Getter
    @Setter
    private Dependency[] dependencies;
}
