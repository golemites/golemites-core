package org.golemites.api;

import java.util.ServiceLoader;

/**
 * TODO: Will be moved to its own bootstrapper.
 */
public class Boot {
    public static ModuleRuntime findModuleRuntime() {
        ServiceLoader<ModuleRuntime> thing = ServiceLoader.load(ModuleRuntime.class);

        for (ModuleRuntime moduleRuntime : thing) {
            return moduleRuntime;
        }
        throw new IllegalStateException("No ModuleRuntime found in classpath.");
    }
}
