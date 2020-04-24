package org.rebaze.osgi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleException;

public class OSGiRuntimeTest {

    @Test
    @Disabled
    void testRuntime() throws Exception {
        OSGiRuntime osgi = null;
        Febo.febo()
                .run(null)
                ;
    }
}
