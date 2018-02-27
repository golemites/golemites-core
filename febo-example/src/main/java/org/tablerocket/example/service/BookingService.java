package org.tablerocket.example.service;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.tablerocket.example.calculator.api.Calculator;
import org.tablerocket.febo.api.FeboEntrypoint;

import java.io.InputStream;
import java.io.PrintStream;

@Component
public class BookingService implements FeboEntrypoint
{
    private BundleContext context;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL) String s;

    @Reference Calculator calculator;

    @Activate
    public void activate(BundleContext ctx) {
        this.context = ctx;
    }

    public void paypload( String s )
    {
        System.out.println("Hello, " + s);

    }

    @Override public void execute( String[] args, InputStream in, PrintStream out, PrintStream err )
    {
        out.println( "Hello from Felix version " + context.getBundle( 0 ).getVersion().toString() );
        out.println(" Sum: " + calculator.sum(2,4));
    }
}
