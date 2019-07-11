package org.tablerocket.febo.api;

import java.io.IOException;
import java.io.InputStream;

public interface Febo extends AutoCloseable {
    void start();

    Febo platform(RepositoryStore repositoryStore);

    Febo require(DelayedBuilder<Dependency> delayed);

    @Deprecated
    Febo require(Dependency... identifiers);

    @Deprecated
    Febo require(String label, InputStream payload) throws IOException;

    void run(String[] args) throws Exception;

    Febo keepRunning(boolean keepRunning);
}
