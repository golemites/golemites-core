package org.tablerocket.febo.plugin.application;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ContainerTest {

    public static final Logger LOG = LoggerFactory.getLogger(ContainerTest.class);

    @Disabled
    @Test
    public void uploadTest() throws IOException {
        ImageBuilder imageBuilder = new ImageBuilder("sample");

        String hash = imageBuilder.containerize(new File("/Users/tonit/devel/rebaze/febo-application/application-service/build/libs/application-service-runner.jar"));
        //assertTrue(hash != null && !hash.isEmpty());
    }
}
