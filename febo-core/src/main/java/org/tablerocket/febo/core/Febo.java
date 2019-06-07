package org.tablerocket.febo.core;

import aQute.lib.io.IO;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.store.Handle;
import org.ops4j.store.Store;
import org.ops4j.store.StoreFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tablerocket.febo.api.DelayedBuilder;
import org.tablerocket.febo.api.Dependency;
import org.tablerocket.febo.api.FeboEntrypoint;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

public class Febo implements AutoCloseable
{
    private final static Logger LOG = LoggerFactory.getLogger( Febo.class );
    private LinkedHashMap<String,Handle> blobindex = new LinkedHashMap<>(  );
    private final Store<InputStream> blobstore;
    private Framework systemBundle;
    private boolean keepRunning = false;

    private Febo() throws BundleException
    {
        this.blobstore = StoreFactory.defaultStore();
    }

    public static Febo febo() throws BundleException
    {
        return new Febo();
    }

    public void start() throws BundleException
    {
        IO.delete( new File("felix-cache") );

        FrameworkFactory factory = ServiceLoader.load( FrameworkFactory.class ).iterator().next();

        @SuppressWarnings({
            "unchecked", "rawtypes"
        })
        Properties p = new Properties();
        p.put( "org.osgi.framework.bootdelegation","org.apache.log4j" );
        p.put( "org.osgi.framework.system.packages.extra","org.tablerocket.febo.api" );

        Map<String,String> configuration = (Map) p;
        systemBundle = factory.newFramework(configuration);
        systemBundle.init();

        System.out.println("\u001B[36m ____  ____  ____   __  \n"
            + "(  __)(  __)(  _ \\ /  \\ \n"
            + " ) _)  ) _)  ) _ ((  O )\n"
            + "(__)  (____)(____/ \\__/ \u001B[0m \u001B[0m\n");

        systemBundle.start();
    }

    private void kill() {
        try
        {
            systemBundle.stop();
        }
        catch ( BundleException e )
        {
            e.printStackTrace();
        }
    }

    @Override public void close()
    {
        kill();
    }

    public Febo require( DelayedBuilder<Dependency> delayed )
    {
        require( delayed.build() );
        return this;
    }

    public Febo require( Dependency... identifiers )

    {
        try
        {
            for (Dependency identifier : identifiers)
            {
                blobindex.put( identifier.identity(),
                    this.blobstore.store( identifier.location().toURL().openStream() ) );
            }
            return this;
        }
        catch ( IOException e )
        {
            throw new RuntimeException(e);
        }

    }

    public Febo require( String label, InputStream payload ) throws IOException
    {
        blobindex.put( label,blobstore.store( payload ) );
        return this;
    }

    public Febo with( String label, TinyBundle tinyBundle ) throws IOException
    {
        blobindex.put( label,blobstore.store( tinyBundle.build( withBnd() ) ) );
        return this;
    }

    private boolean bounce()
    {
        boolean success = true;
        for (Bundle b : systemBundle.getBundleContext().getBundles()) {
            try
            {
                String fragmentHost = b.getHeaders().get( Constants.FRAGMENT_HOST );
                if (fragmentHost == null)
                {
                    b.start();
                }
            }
            catch ( BundleException e )
            {
                success = false;
                throw new RuntimeException("Unable to start bundle " + b.getSymbolicName() + " ("+e.getMessage()+")",e);

            }
        }
        return success;
    }

    private <T> T entrypoint( Class<T> entryClass )
    {
        //Thread.currentThread().setContextClassLoader( systemBundle.getClass().getClassLoader() );

        ServiceTracker tracker = null;
        try
        {
            tracker = new ServiceTracker(
                systemBundle.getBundleContext(),
                systemBundle.getBundleContext().createFilter("(objectClass=" + entryClass.getName() + ")")
                ,null);
            tracker.open();

            T service = ( T ) tracker.waitForService( 2000 );
            if (service == null) {
                throw new IllegalStateException( "Entrypoint " + entryClass.getName() + " is not available." );
            }
            return service;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }finally {
            if (tracker != null) {
                tracker.close();
            }
        }
    }

    /**
     * Marks the end of preparing the instance.
     * By this time, dependencies must have met and the entrypoint must be reachable.
     * Otherwise this will raise an exception.
     */
    public synchronized void run(String[] args) throws Exception
    {
        boolean success = false;
        try
        {
            Instant t = Instant.now();
            start();
            for (Map.Entry<String, Handle> entry : blobindex.entrySet()) {
                Bundle b = systemBundle.getBundleContext().installBundle(  blobstore.getLocation(entry.getValue()).toASCIIString(),blobstore.load(entry.getValue()) );
                LOG.debug("Installed " + b.getSymbolicName() + " in version " + b.getVersion() + " from " +  b.getLocation());
                scan(b);
            }
            success = bounce();
            if (success)
            {
                FeboEntrypoint entry = entrypoint( FeboEntrypoint.class );
                String version = systemBundle.getHeaders().get( Constants.BUNDLE_VERSION );
                System.out.println("\u001B[36mBooted FEBO on Apache Felix " + version + " in " + Duration.between(t, Instant.now()).toMillis() + " ms.\u001B[0m \u001B[0m\r\n");

                //System.out.println("System booted in " + Duration.between(t, Instant.now()).toMillis() + " ms.");
                entry.execute( args, System.in, System.out, System.err );
            }
        }finally
        {
            if (!success || !keepRunning)
            {
                close();
            }
        }
    }

    private void scan(Bundle b) {
        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream( new URL(b.getLocation()).openStream());

        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory() && entry.getName().startsWith("OSGI-INF/") && entry.getName().endsWith(".xml")) {
                LOG.debug(" + " + entry.getName());

                try (BufferedReader br = new BufferedReader(new InputStreamReader(b.getResource(entry.getName()).openStream()))) {
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        if (LOG.isTraceEnabled()) {
                            System.out.println(line);
                        }
                    }
                }
            }
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Febo keepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
        return this;
    }

}
