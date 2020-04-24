package org.rebaze.osgi.testbundles;

import org.golemites.api.Entrypoint;
import org.osgi.service.component.annotations.Component;

import java.io.InputStream;
import java.io.PrintStream;

@Component(immediate = true)
public class DemoEntry implements Entrypoint {
    @Override
    public void execute(String[] args, InputStream in, PrintStream out, PrintStream err) {

    }
}
