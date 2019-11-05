package org.golemites.api;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Entrypoint makes a febo application a command.
 * If existing, it will run and framework will shutdown automatically.
 */
public interface Entrypoint
{
    void execute( String[] args, InputStream in, PrintStream out, PrintStream err );
}
