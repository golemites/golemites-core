package org.tablerocket.febo.api;

import java.io.InputStream;
import java.io.PrintStream;

public interface FeboEntrypoint
{
    void execute( String[] args, InputStream in, PrintStream out, PrintStream err );
}
