package org.tablerocket.febo.core;

import org.osgi.framework.BundleException;
import org.tablerocket.febo.api.FeboFactory;

public class FeboApplication {
    public static Febo run(Class<? extends FeboFactory> primary, String[] args) throws BundleException {
        Febo febo = Febo.febo();
        try {
            FeboFactory ff = primary.getDeclaredConstructor().newInstance();
            ff.configure(febo);
            febo.run(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return febo;
    }
}
