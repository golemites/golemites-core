package org.tablerocket.febo.core;

import org.apache.felix.framework.util.FelixConstants;
import org.ops4j.io.FileUtils;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.store.Store;
import org.ops4j.store.StoreFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tablerocket.febo.api.FeboEntrypoint;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

public class Febo implements AutoCloseable
{
    public final static Logger LOG = LoggerFactory.getLogger( Febo.class );
    private final Repository repository;
    private final Store<InputStream> blobstore;
    private Framework systemBundle;

    private Febo(Repository repository) throws BundleException
    {
        this.repository = repository;
        this.blobstore = StoreFactory.defaultStore();
        start();
    }

    public static Febo febo(Repository repo) throws BundleException
    {
        return new Febo(repo);
    }

    public void start() throws BundleException
    {
        System.out.println(" ____  ____  ____   __  \n"
            + "(  __)(  __)(  _ \\ /  \\ \n"
            + " ) _)  ) _)  ) _ ((  O )\n"
            + "(__)  (____)(____/ \\__/ \n");

        FileUtils.delete( new File("felix-cache") );

        FrameworkFactory factory = ServiceLoader.load( FrameworkFactory.class ).iterator().next();

        @SuppressWarnings({
            "unchecked", "rawtypes"
        })
        Properties p = new Properties();
        //p.put( "org.osgi.framework.bootdelegation","org.tablerocket.febo.api" );
        p.put( "org.osgi.framework.system.packages.extra","org.tablerocket.febo.api" );

        Map<String,String> configuration = (Map) p;
        systemBundle = factory.newFramework(configuration);
        systemBundle.init();
        systemBundle.start();
    }

    public void kill() {
        try
        {
            systemBundle.stop();
        }
        catch ( BundleException e )
        {
            e.printStackTrace();
        }
    }

    @Override public void close() throws Exception
    {
        kill();
    }

    public Febo demand( Dependency identifier )
    {
        try
        {
             systemBundle.getBundleContext().installBundle( identifier.identity(),identifier.location().toURL().openStream() );
            return this;
        }
        catch ( Exception e )
        {
            throw new RuntimeException(e);
        }

    }

    public Febo install( String label, InputStream payload ) throws BundleException
    {
        systemBundle.getBundleContext().installBundle( label,payload );
        return this;
    }

    public Febo install( String label, TinyBundle tinyBundle ) throws BundleException
    {
        install( label,tinyBundle.build(withBnd()) );
        return this;
    }

    private void bounce()
    {
        for (Bundle b : systemBundle.getBundleContext().getBundles()) {
            try
            {
                b.start();
            }
            catch ( BundleException e )
            {
                LOG.warn("Unable to start bundle " + b.getSymbolicName() + " ("+e.getMessage()+")",e);
            }
        }
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
    public void run(String[] args) throws Exception
    {
        try
        {
            bounce();
            FeboEntrypoint entry = entrypoint( FeboEntrypoint.class );
            entry.execute( args, System.in, System.out, System.err );
        }finally
        {
            close();
        }
    }
}
