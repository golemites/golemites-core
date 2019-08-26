package org.golemites.api;

import lombok.Builder;
import lombok.Data;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

@Data
@Builder
public class ApplicationImage {
    private Path spec;
    private List<Path> platformArtifacts;
    private List<Path> applicationArtifacts;
    private URI launcher;
}
