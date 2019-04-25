package org.tablerocket.example.calculator;

import org.junit.jupiter.api.Test;
import org.tablerocket.example.FeboRepository;
import org.tablerocket.example.mytest.MyTestEntry;
import org.tablerocket.febo.autobundle.AutoBundleSupport;
import org.tablerocket.febo.repository.ClasspathRepositoryStore;

import static org.tablerocket.febo.core.Febo.febo;

class CalcTest {

    @Test
    void simpleTest() throws Exception {
        FeboRepository repo = new FeboRepository(new ClasspathRepositoryStore());

        // Optional concept from febo-autobundle allowing bundleization of parts of the classpath from here based on convention.
        AutoBundleSupport autoBundle = new AutoBundleSupport();
        febo()
                .require(repo.org_apache_felix_configadmin())
                .require(repo.org_osgi_util_function())
                .require(repo.org_osgi_util_promise())
                .require(repo.org_apache_felix_scr())
                .require(autoBundle.from(MyTestEntry.class))
                .require(autoBundle.from(CalculatorBundle.class).withAutoExportApi(true))
                .run(new String[]{});
    }
}
