package org.golemites.api;

import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 * TODO: Will be moved to its own bootstrapper.
 */
public class Boot {
    public static ModuleRuntime findModuleRuntime(ClassLoader cl) {
        ServiceLoader<ModuleRuntime> runtimesFound = ServiceLoader.load(ModuleRuntime.class,cl);
        return StreamSupport.stream(runtimesFound.spliterator(),false).findFirst().orElseThrow(IllegalStateException::new);
    }
}
