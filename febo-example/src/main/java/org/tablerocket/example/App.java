package org.tablerocket.example;

import org.tablerocket.example.calculator.CalculatorBundle;
import org.tablerocket.example.service.ExampleBundle;
import org.tablerocket.febo.repository.AutoBundleSupport;

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.tablerocket.febo.core.Febo.febo;

public class App
{
    public static void main( String[] args ) throws Exception
    {
        FeboRepository repo = new FeboRepository();

        AutoBundleSupport autoBundle = new AutoBundleSupport();

        febo()
            .require( repo.org_apache_felix_configadmin() )
            .require( repo.org_apache_felix_scr() )
            .require( autoBundle.from( ExampleBundle.class ) )
            .require( autoBundle.from( CalculatorBundle.class ) )

            //.with( "booking", bundle().add( BookingService.class ) )
            .run( args );
    }
}
