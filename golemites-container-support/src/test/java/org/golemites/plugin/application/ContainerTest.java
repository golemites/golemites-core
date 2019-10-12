package org.golemites.plugin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.ApiException;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.PushTarget;
import org.golemites.api.TargetPlatformSpec;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class ContainerTest {

    public static final Logger LOG = LoggerFactory.getLogger(ContainerTest.class);

    @Disabled
    @Test
    public void imageBuilerTest() throws IOException, ApiException {
        // TODO: build synthetic test jars to be used here.
        File out = File.createTempFile("febotest",".jar",new File("build/"));

        LOG.info("Wrtiting " + out.getAbsolutePath());
        GolemitesApplicationExtension ext = new GolemitesApplicationExtension();
        //ext.setRepository("rebaze-camp-dev.jfrog.io/app:latest");
        ext.setRepository("eu.gcr.io/golemite/application-service");
        ext.setPushTo(PushTarget.REGISTRY);
        ext.setName("sample");

        ImageBuilder imageBuilder = new ImageBuilder(out.toPath());

        TargetPlatformSpec spec = imageBuilder.findSpec(Collections.singletonList(new File("./../golemites-example-baseline/build/libs/golemites-example-baseline-0.1.0-SNAPSHOT.jar").toURI()));

        TargetPlatformSpec result = imageBuilder.prepare(
                spec
        );
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));

        //imageBuilder.deploy("@sha256:" + result.getImageID());
    }

    @Disabled
    @Test
    public void deployTest() throws IOException, ApiException {
        // TODO: build synthetic test jars to be used here.

        GolemitesApplicationExtension ext = new GolemitesApplicationExtension();
        ext.setRepository("eu.gcr.io/golemite/application-service");
        ext.setName("sample");

        ImageBuilder imageBuilder = new ImageBuilder(null);
        //imageBuilder.("@sha256:07a6e7fc35893ef30ed0d3432cebc490512d1dc651a98d6beb78e1e2d001537c");
    }

}
