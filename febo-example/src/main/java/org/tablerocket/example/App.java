package org.tablerocket.example;

import org.tablerocket.example.calculator.CalculatorBundle;
import org.tablerocket.example.service.ExampleBundle;
import org.tablerocket.febo.autobundle.AutoBundleSupport;

import static org.tablerocket.febo.core.Febo.febo;

/**
 * Minimal example application showing the basic setup and some optional concepts that complement febo core.
 */
public class App
{
    public static void main( String[] args ) throws Exception
    {
        // Optional concept from febo-repository giving static access to bundle resources loaded by gradle plugin.
        FeboRepository repo = new FeboRepository();

        // Optional concept from febo-autobundle allowing bundleization of parts of the classpath from here based on convention.
        AutoBundleSupport autoBundle = new AutoBundleSupport();

        febo()
            .require( repo.org_apache_felix_configadmin() )
            .require( repo.org_apache_felix_scr() )
            .require( autoBundle.from( ExampleBundle.class ) )
            .require( autoBundle.from( CalculatorBundle.class ).withAutoExportApi( true ) )
            .run( args );
    }
}
