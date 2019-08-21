package org.golemites.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public interface Febo extends AutoCloseable {

    Febo exposePackage(String p);

    boolean start() throws IOException;

    Febo platform(TargetPlatformSpec platform);

    Febo require(DelayedBuilder<Dependency>... delayed);

    Febo require(Collection<DelayedBuilder<Dependency>> delayed);

    @Deprecated
    Febo require(Dependency... identifiers);

    <T> Optional<T> service(Class<T> clazz) throws Exception;

    void stop();
}
