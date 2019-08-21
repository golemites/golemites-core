package org.golemites.api;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Febo Entrypoint makes a febo application a command.
 * If existing, it will run and framework will shutdown automatically.
 */
public interface FeboEntrypoint
{
    void execute( String[] args, InputStream in, PrintStream out, PrintStream err );
}
