package org.tablerocket.febo.plugin.application;

import com.google.cloud.tools.jib.api.*;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class ImageBuilder {

    public static final String BASE_IMAGE = "openjdk:8-jre-alpine";
    public static final String LATEST_VERSION = "latest";
    public static final String JAVA_PATH = "/usr/bin/java";

    private final String name;

    public ImageBuilder(String name) {
        this.name = name;
    }

    public String containerize(File output) throws IOException {
        // TODO: add dependencies into separate layers.
        try {
            ImageReference ref = ImageReference.parse("401110809112.dkr.ecr.eu-central-1.amazonaws.com/camp/" + name + ":latest");
            JibContainer result = Jib.from(BASE_IMAGE)
                    .addLayer(Collections.singletonList(output.toPath()), AbsoluteUnixPath.get("/"))
                    .setEntrypoint(JAVA_PATH, "-jar", "/" + output.getName())
                    .setCreationTime(Instant.now())
                    .containerize(Containerizer.to(RegistryImage.named(ref)
                            .addCredentialRetriever(CredentialRetrieverFactory.forImage(ref).dockerConfig())));
            return result.getImageId().getHash();
        } catch (InterruptedException | RegistryException | CacheDirectoryCreationException | ExecutionException | InvalidImageReferenceException e) {
            // getLogger().error("Problem creating image",e);
            throw new RuntimeException(e);
        }
    }
}
