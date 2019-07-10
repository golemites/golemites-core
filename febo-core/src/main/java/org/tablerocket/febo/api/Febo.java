package org.tablerocket.febo.api;

import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.BundleException;

import java.io.IOException;
import java.io.InputStream;

public interface Febo extends AutoCloseable {
    void start() throws BundleException;

    Febo require(DelayedBuilder<Dependency> delayed);

    Febo require(Dependency... identifiers);

    Febo require(String label, InputStream payload) throws IOException;

    Febo with(String label, TinyBundle tinyBundle) throws IOException;

    void run(String[] args) throws Exception;
}
