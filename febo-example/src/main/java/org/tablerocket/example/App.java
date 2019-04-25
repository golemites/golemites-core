package org.tablerocket.example;

import org.tablerocket.example.calculator.CalculatorBundle;
import org.tablerocket.example.service.BookingService;
import org.tablerocket.febo.api.FeboFactory;
import org.tablerocket.febo.autobundle.AutoBundleSupport;
import org.tablerocket.febo.core.Febo;
import org.tablerocket.febo.core.FeboApplication;
import org.tablerocket.febo.repository.ClasspathRepositoryStore;

/**
 * Minimal example application showing the basic setup and some optional concepts that complement febo core.
 */
public class App implements FeboFactory {

    public static void main(String[] args) throws Exception {
        FeboApplication.run(App.class,args);
    }

    @Override
    public void configure(Febo febo) {
        // Optional concept from febo-repository giving static access to bundle resources loaded by gradle plugin.
        FeboRepository repo = new FeboRepository(new ClasspathRepositoryStore());
        // Optional concept from febo-autobundle allowing bundleization of parts of the classpath from here based on convention.
        AutoBundleSupport autoBundle = new AutoBundleSupport();
        febo
                .require(repo.org_osgi_util_function())
                .require(repo.org_osgi_util_promise())
                .require(repo.org_apache_felix_scr())
                .require(autoBundle.from(BookingService.class))
                .require(autoBundle.from(CalculatorBundle.class).withAutoExportApi(true));
    }
}
