package org.golemites.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public interface ModuleRuntime extends AutoCloseable {

    ModuleRuntime exposePackage(String p);

    boolean start() throws IOException;

    ModuleRuntime platform(TargetPlatformSpec platform);

    ModuleRuntime require(DelayedBuilder<Dependency>... delayed);

    ModuleRuntime require(Collection<DelayedBuilder<Dependency>> delayed);

    @Deprecated
    ModuleRuntime require(Dependency... identifiers);

    <T> Optional<T> service(Class<T> clazz) throws Exception;

    void stop();
}
