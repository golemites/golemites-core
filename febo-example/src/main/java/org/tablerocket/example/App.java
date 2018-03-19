package org.tablerocket.example;

import org.tablerocket.example.calculator.CalculatorBundle;
import org.tablerocket.example.service.ExampleBundle;
import org.tablerocket.febo.autobundle.AutoBundleSupport;
import org.tablerocket.febo.autobundle.BndProject;
import org.tablerocket.febo.core.ResolvedDependency;
import org.tablerocket.febo.repository.ClasspathRepositoryStore;

import java.io.File;

import static org.tablerocket.febo.core.Febo.febo;

/**
 * Minimal example application showing the basic setup and some optional concepts that complement febo core.
 */
public class App
{
    public static void main( String[] args ) throws Exception
    {
        // Optional concept from febo-repository giving static access to bundle resources loaded by gradle plugin.
        FeboRepository repo = new FeboRepository(new ClasspathRepositoryStore());

        BndProject bndProject = new BndProject();


        // Optional concept from febo-autobundle allowing bundleization of parts of the classpath from here based on convention.
        AutoBundleSupport autoBundle = new AutoBundleSupport();

        febo()
            //.require( repo.febo_example_bundle() )
            .require( repo.org_apache_felix_scr() )
            //.require( bndProject.from("febo-example-bundle") )
            //.require( new ResolvedDependency( "example-bundle",new File("febo-example-bundle/build/libs/febo-example-bundle-0.1.0-SNAPSHOT.jar").toURI() ) )
            .require( autoBundle.from( ExampleBundle.class ) )
            .require( autoBundle.from( CalculatorBundle.class ).withAutoExportApi( true ) )
            .run( args );
    }
}
