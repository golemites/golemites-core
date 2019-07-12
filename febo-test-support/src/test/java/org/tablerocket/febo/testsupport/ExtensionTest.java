package org.tablerocket.febo.testsupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(FeboExtension.class)
 class ExtensionTest {

    private static Logger LOG = LoggerFactory.getLogger(ExtensionTest.class);

    @Test
    void test1(DemoService service) {
        LOG.info("Executing test with injected: " + service);
    }

    @Test
    void test2(DemoService service) {
        LOG.info("Executing test with injected: " + service);
    }
}
