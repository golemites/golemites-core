package org.rebaze.osgi;

import org.golemites.autobundle.AutoBundleSupport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleException;
import org.rebaze.osgi.testbundles.OneBundle;

public class OSGiRuntimeTest {

    @Test
    @Disabled
    void testRuntime() throws Exception {
        OSGiRuntime osgi = null;
        AutoBundleSupport auto = new AutoBundleSupport();
        Febo.febo()
                .require(auto.from(OneBundle.class).withBndHeader("Export-Package",OneBundle.class.getPackage().getName()))
                .run(null)
                ;
    }
}
