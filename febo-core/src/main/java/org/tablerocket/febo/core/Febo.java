package org.tablerocket.febo.core;

import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

public class Febo implements AutoCloseable
{
    public final static Logger LOG = LoggerFactory.getLogger( Febo.class );
    private final Repository repository;
    private Framework systemBundle;

    public Febo(Repository repository) throws BundleException
    {
        this.repository = repository;
        start();
    }

    public void start() throws BundleException
    {
        FrameworkFactory factory = ServiceLoader.load( FrameworkFactory.class ).iterator().next();

        @SuppressWarnings({
            "unchecked", "rawtypes"
        })
        Properties p = new Properties();
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

    public Bundle demand( Dependency identifier )
    {
        try
        {
            return systemBundle.getBundleContext().installBundle( identifier.identity(),identifier.location().toURL().openStream() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException(e);
        }

    }

    public Bundle install( String label, InputStream payload ) throws BundleException
    {
        return systemBundle.getBundleContext().installBundle( label,payload );
    }

    public Bundle install( String label, TinyBundle tinyBundle ) throws BundleException
    {
        return systemBundle.getBundleContext().installBundle( label,tinyBundle.build() );
    }

    public void bounce()
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
}
