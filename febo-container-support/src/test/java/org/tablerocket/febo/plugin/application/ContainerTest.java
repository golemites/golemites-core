package org.tablerocket.febo.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tablerocket.febo.api.FeboApplicationExtension;
import org.tablerocket.febo.api.TargetPlatformSpec;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class ContainerTest {

    public static final Logger LOG = LoggerFactory.getLogger(ContainerTest.class);

    @Disabled
    @Test
    public void uploadTest() throws IOException {
        FeboApplicationExtension ext = new FeboApplicationExtension();
        ext.setRepository("rebaze-camp-dev.jfrog.io/app:latest");
        //ext.setRepository("quay.io/rebaze/camp-dev:latest");
        ImageBuilder imageBuilder = new ImageBuilder("sample",ext);

        String hash = imageBuilder.containerize(new File("/Users/tonit/devel/rebaze/febo-application/application-service/build/libs/application-service-runner.jar"));
        //assertTrue(hash != null && !hash.isEmpty());
    }

    @Test
    public void imageBuilerTest() throws IOException, URISyntaxException {
        File out = File.createTempFile("febotest",".jar",new File("build/"));
        LOG.info("Wrtiting " + out.getAbsolutePath());
        FeboApplicationExtension ext = new FeboApplicationExtension();
        ext.setRepository("rebaze-camp-dev.jfrog.io/app:latest");
        ext.setDeployImage(false);
        ImageBuilder imageBuilder = new ImageBuilder("sample",ext);

        TargetPlatformSpec result = imageBuilder.buildRunnerJar(out,
                new File("./../febo-osgi-launcher/build/libs/febo-osgi-launcher-0.1.0-SNAPSHOT.jar").toURI(),
                Arrays.asList(
                        new File("./../febo-example-baseline/build/libs/febo-example-baseline-0.1.0-SNAPSHOT.jar").toURI()
                        //new File("").toURI(),
                        //new File("").toURI()
                )
        );
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));

    }
}
