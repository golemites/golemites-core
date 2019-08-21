package org.golemites.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.golemites.api.Boot;
import org.golemites.api.Febo;
import org.golemites.api.FeboEntrypoint;
import org.golemites.repository.EmbeddedStore;

import java.util.Optional;

public class Launcher {
    private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws Exception {
        // this needs to have platform + application bundles in blob.
        Febo febo = Boot.febo().platform(new EmbeddedStore().platform());

        febo.start();
        Optional<FeboEntrypoint> command = febo.service(FeboEntrypoint.class);
        if (command.isPresent()) {
            LOG.info("Found command, executing..");
            command.get().execute(args,System.in, System.out,System.err);
            // hard exit
            System.exit(0);

            febo.stop();
        }else {
            LOG.info("Golemites is running in server mode.");
        }
    }
}
