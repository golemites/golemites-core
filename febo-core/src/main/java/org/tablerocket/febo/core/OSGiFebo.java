package org.tablerocket.febo.core;

import aQute.lib.io.IO;
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
import org.tablerocket.febo.api.*;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OSGiFebo implements Febo {
    private final static Logger LOG = LoggerFactory.getLogger( OSGiFebo.class );
    private LinkedHashMap<String,Handle> blobindex = new LinkedHashMap<>(  );
    private final Store<InputStream> blobstore;
    private Framework systemBundle;
    private Set<String> packagesExposed = new HashSet<>();

    public OSGiFebo() {
        this.blobstore = StoreFactory.defaultStore();
    }

    public static Febo febo() {
        return new OSGiFebo();
    }

    @Override
    public Febo exposePackage(String p) {
        this.packagesExposed.add(p);
        return this;
    }

    @Override
    public boolean start()
    {
        Instant t = Instant.now();
        IO.delete( new File("felix-cache") );
        systemBundle = configureFramework();

        try {
            systemBundle.init();
            System.out.println("\u001B[36m ____  ____  ____   __  \n"
                + "(  __)(  __)(  _ \\ /  \\ \n"
                + " ) _)  ) _)  ) _ ((  O )\n"
                + "(__)  (____)(____/ \\__/ \u001B[0m \u001B[0m\n");

            systemBundle.start();
            for (Map.Entry<String, Handle> entry : blobindex.entrySet()) {
                Bundle b = systemBundle.getBundleContext().installBundle(  blobstore.getLocation(entry.getValue()).toASCIIString(),blobstore.load(entry.getValue()) );
                LOG.debug("Installed " + b.getSymbolicName() + " in version " + b.getVersion() + " from " +  b.getLocation());
                //scan(b);
            }
            boolean success = bounce();
            if (success) {
                String version = systemBundle.getHeaders().get( Constants.BUNDLE_VERSION );
                LOG.info("\u001B[36mBooted FEBO on Apache Felix " + version + " in " + Duration.between(t, Instant.now()).toMillis() + " ms.\u001B[0m \u001B[0m\r\n");
            }
            return success;
        } catch (BundleException | IOException e) {
            throw new RuntimeException("OSGI Framework did not boot..",e);
        }
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private Framework configureFramework() {
        FrameworkFactory factory = ServiceLoader.load( FrameworkFactory.class ).iterator().next();
        Properties p = new Properties();
        p.put( "org.ops4j.pax.logging.DefaultServiceLog.level","WARN" );
        exposePackage("org.tablerocket.febo.api");
        String extraPackages = String.join(",",packagesExposed);
        p.put( "org.osgi.framework.system.packages.extra",extraPackages );
        return factory.newFramework((Map) p);
    }

    @Override
    public Febo platform(TargetPlatformSpec platform) {
        return require(platform.getDependencies());
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
    public Febo require(DelayedBuilder<Dependency>... delayed)
    {
        Arrays.asList(delayed).forEach( d -> require(d.build()));
        return this;
    }

    @Override
    public Febo require(Collection<DelayedBuilder<Dependency>> delayed)
    {
        delayed.forEach( d -> require(d.build()));
        return this;
    }

    @Override
    public Febo require(Dependency... identifiers)
    {
        try
        {
            for (Dependency identifier : identifiers)
            {
                LOG.debug("Adding platform dependency: " + identifier.getLocation().toASCIIString());
                blobindex.put( identifier.getIdentity(),
                    this.blobstore.store( identifier.getLocation().toURL().openStream() ) );
            }
            return this;
        }
        catch ( IOException e )
        {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Febo require(String label, InputStream payload) throws IOException
    {
        blobindex.put( label,blobstore.store( payload ) );
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
        Optional<FeboEntrypoint> entry = entrypoint( FeboEntrypoint.class );
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

}
