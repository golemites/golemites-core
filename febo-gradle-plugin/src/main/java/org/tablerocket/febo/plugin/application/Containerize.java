package org.tablerocket.febo.plugin.application;

import com.google.cloud.tools.jib.api.*;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class Containerize {
    public String containerize(File output) throws IOException {
        // TODO: add dependencies into separate layers.
        try {
            JibContainer result = Jib.from("openjdk:8-jre-alpine")
                    .addLayer(Collections.singletonList(output.toPath()), AbsoluteUnixPath.get("/"))
                    .setEntrypoint("/usr/bin/java", "-jar", "/" + output.getName())
                    .setCreationTime(Instant.now())
                    .containerize(
                            Containerizer.to(DockerDaemonImage.named("foo:latest")
                            ));
            return result.getImageId().getHash();
        } catch (InterruptedException | RegistryException | CacheDirectoryCreationException | ExecutionException | InvalidImageReferenceException e) {
            // getLogger().error("Problem creating image",e);
            throw new RuntimeException(e);
        }
    }
}
