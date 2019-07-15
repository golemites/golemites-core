package org.tablerocket.febo.launcher;

import org.tablerocket.febo.api.Boot;
import org.tablerocket.febo.api.Febo;
import org.tablerocket.febo.repository.EmbeddedStore;

public class Launcher {
    public static void main(String[] args) throws Exception {
        Febo febo = Boot.febo()
                .platform(new EmbeddedStore()) // this needs to have platform + application bundles in blob.
                .keepRunning(true);
        febo.run(args);
    }
}
