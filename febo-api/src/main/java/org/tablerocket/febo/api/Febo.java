package org.tablerocket.febo.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

public interface Febo extends AutoCloseable {

    Febo exposePackage(String p);

    boolean start();

    Febo platform(TargetPlatformSpec platform);

    Febo require(DelayedBuilder<Dependency>... delayed);

    Febo require(Collection<DelayedBuilder<Dependency>> delayed);

    @Deprecated
    Febo require(Dependency... identifiers);

    @Deprecated
    Febo require(String label, InputStream payload) throws IOException;

    <T> Optional<T> service(Class<T> clazz) throws Exception;

    void stop();
}
