package org.golemites.core;

import org.golemites.api.DelayedBuilder;
import org.golemites.api.Dependency;
import org.golemites.api.Entrypoint;
import org.golemites.api.ModuleRuntime;
import org.golemites.api.TargetPlatformSpec;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

public class OSGiModuleRuntime implements ModuleRuntime {
    private final static Logger LOG = LoggerFactory.getLogger( OSGiModuleRuntime.class );
    private Map<String,Dependency> deps = new LinkedHashMap<>(  );
    private Framework systemBundle;
    private Set<String> packagesExposed = new HashSet<>();

    public static ModuleRuntime create() {
        return new OSGiModuleRuntime();
    }

    @Override
    public ModuleRuntime exposePackage(String p) {
        this.packagesExposed.add(p);
        return this;
    }

    @Override
    public boolean start() throws IOException {
        Instant t = Instant.now();
        Helpers.delete( new File("felix-cache") );
        systemBundle = configureFramework();

        try {
            systemBundle.init();
            systemBundle.start();
            for (Map.Entry<String, Dependency> entry : deps.entrySet()) {
                try (InputStream is = open(entry.getValue())) {
                    Bundle b = systemBundle.getBundleContext().installBundle(entry.getKey(), is);
                    LOG.debug("Installed " + b.getSymbolicName() + " in version " + b.getVersion() + " from " + b.getLocation());
                }
                //scan(b);
            }
            boolean success = bounce();
            if (success) {
                String version = systemBundle.getHeaders().get( Constants.BUNDLE_VERSION );
                LOG.info("\u001B[36mBooted GOLEMITES on Apache Felix " + version + " in " + Duration.between(t, Instant.now()).toMillis() + " ms.\u001B[0m \u001B[0m\r\n");
            }
            return success;
        } catch (BundleException | IOException e) {
            throw new RuntimeException("OSGI Framework did not boot..",e);
        }
    }

    private InputStream open(Dependency dependency) throws FileNotFoundException {
        URI location = dependency.getLocation();
        File local = new File(location);
        if (!local.exists()) {
            File relative = new File(".",local.getAbsolutePath());
            if (relative.exists()) {
                local = relative;
            }else {
                throw new RuntimeException("Cannot load dependency from spec: " + dependency);
            }
        }
        //File local = new File(".",dependency.getLocation().toASCIIString()).getAbsoluteFile();
        LOG.debug("Trying to install " + local);
        return new FileInputStream(local);
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private Framework configureFramework() {
        FrameworkFactory factory = ServiceLoader.load( FrameworkFactory.class ).iterator().next();
        Properties p = new Properties();
        p.put( "org.ops4j.pax.logging.DefaultServiceLog.level","INFO" );
        exposePackage("org.golemites.api");
        String extraPackages = String.join(",",packagesExposed);
        p.put( "org.osgi.framework.system.packages.extra",extraPackages );
        LOG.warn("Log settings are here: " + new File("log4j.properties" ).getAbsolutePath());
        p.put( "org.ops4j.pax.logging.property.file",new File("log4j.properties" ).getAbsolutePath());

        return factory.newFramework((Map) p);
    }

    @Override
    public ModuleRuntime platform(TargetPlatformSpec platform) {
        require(platform.getDependencies());
        if (platform.getApplication() != null) {
            require(platform.getApplication());
        }
        return this;
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

    @Override
    public ModuleRuntime require(DelayedBuilder<Dependency>... delayed)
    {
        Arrays.asList(delayed).forEach( d -> require(d.build()));
        return this;
    }

    @Override
    public ModuleRuntime require(Collection<DelayedBuilder<Dependency>> delayed)
    {
        delayed.forEach( d -> require(d.build()));
        return this;
    }

    @Override
    public ModuleRuntime require(Dependency... identifiers)
    {
        for (Dependency identifier : identifiers)
        {
            LOG.debug("Adding platform dependency: " + identifier.getLocation().toASCIIString());
            deps.put( identifier.getIdentity(),identifier);

        }
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

    private <T> Optional<T> entrypoint( Class<T> entryClass )
    {
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
                return Optional.empty();
            }
            return Optional.of(service);
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

    private void callEntrypoint(String[] args) {
        Optional<Entrypoint> entry = entrypoint( Entrypoint.class );
        if (entry.isPresent()) {
            LOG.info("Entrypoint is " + entry.getClass().getName());
            entry.get().execute(args, System.in, System.out, System.err);
        }
    }

    @Override
    public <T> Optional<T> service(Class<T> clazz) {
        return entrypoint(clazz);
    }

    @Override
    public void stop() {
        kill();
    }

}
