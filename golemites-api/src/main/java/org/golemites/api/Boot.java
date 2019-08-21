package org.golemites.api;

import java.util.ServiceLoader;

/**
 * TODO: Will be moved to its own bootstrapper.
 */
public class Boot {
    public static Febo febo() {
        ServiceLoader<Febo> thing = ServiceLoader.load(Febo.class);

        for (Febo febo : thing) {
            return febo;
        }
        throw new IllegalStateException("No Febo Runtime found in classpath.");
    }
}
