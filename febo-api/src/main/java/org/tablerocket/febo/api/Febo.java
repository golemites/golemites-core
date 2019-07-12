package org.tablerocket.febo.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public interface Febo extends AutoCloseable {

    Febo exposePackage(String p);

    void start();

    Febo platform(RepositoryStore repositoryStore);

    Febo require(DelayedBuilder<Dependency>... delayed);

    Febo require(Collection<DelayedBuilder<Dependency>> delayed);

    @Deprecated
    Febo require(Dependency... identifiers);

    @Deprecated
    Febo require(String label, InputStream payload) throws IOException;

    void run(String[] args) throws Exception;

    <T> T service(Class<T> clazz) throws Exception;


    void stop();

    Febo keepRunning(boolean keepRunning);
}
