package org.golemites.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.PushTarget;
import org.golemites.api.TargetPlatformSpec;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContainerTest {

    public static final Logger LOG = LoggerFactory.getLogger(ContainerTest.class);

    @Test
    public void uriConverter() throws IOException {
        File s = new File("APPLICATION/foo.jar");
        URI uri = URI.create("file:///APPLICATION/foo.jar");
        assertEquals("file:///APPLICATION/foo.jar",uri.toASCIIString());
        File f = new File(uri);
        assertEquals("foo.jar",f.getName());

    }

    @Test
    public void imageBuilerTest() throws IOException {
        // TODO: build synthetic test jars to be used here.
        File out = File.createTempFile("febotest",".jar",new File("build/"));

        LOG.info("Wrtiting " + out.getAbsolutePath());
        GolemitesApplicationExtension ext = new GolemitesApplicationExtension();
        //ext.setRepository("rebaze-camp-dev.jfrog.io/app:latest");
        ext.setRepository("app:latest");
        ext.setDeployImage(true);
        ext.setPushTo(PushTarget.DOCKER_DAEMON);

        ImageBuilder imageBuilder = new ImageBuilder("sample",ext);

        TargetPlatformSpec spec = imageBuilder.findSpec(Collections.singletonList(new File("./../golemites-example-baseline/build/libs/golemites-example-baseline-0.1.0-SNAPSHOT.jar").toURI()));

        TargetPlatformSpec result = imageBuilder.build(out,
                new File("./../golemites-osgi-launcher/build/libs/golemites-osgi-launcher-0.1.0-SNAPSHOT.jar").toURI(),
                spec,
                Arrays.asList(
                        new File("./../../febo-application/application-calculator/build/classes/java/main").toURI(),
                        new File("./../../febo-application/application-web/build/classes/java/main").toURI(),
                        new File("./../../febo-application/application-service/build/classes/java/main").toURI()
                )
        );
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));

    }
}
