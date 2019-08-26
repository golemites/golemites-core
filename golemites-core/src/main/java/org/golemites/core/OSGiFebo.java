package org.golemites.core;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.golemites.api.DelayedBuilder;
import org.golemites.api.Dependency;
import org.golemites.api.Febo;
import org.golemites.api.FeboEntrypoint;
import org.golemites.api.TargetPlatformSpec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OSGiFebo implements Febo {
    private final static Logger LOG = LoggerFactory.getLogger( OSGiFebo.class );
    private Map<String,Dependency> deps = new LinkedHashMap<>(  );
    private Framework systemBundle;
    private Set<String> packagesExposed = new HashSet<>();

    public OSGiFebo() {
        //this.blobstore = StoreFactory.defaultStore();
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
    public boolean start() throws IOException {
        Instant t = Instant.now();
        delete( new File("felix-cache") );
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

    private void delete(File file) throws IOException {
        if (file.exists() && file.isDirectory()) {
            Path pathToBeDeleted = file.toPath();

            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
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
        p.put( "org.ops4j.pax.logging.DefaultServiceLog.level","WARN" );
        exposePackage("org.golemites.api");
        String extraPackages = String.join(",",packagesExposed);
        p.put( "org.osgi.framework.system.packages.extra",extraPackages );
        LOG.warn("Log settings are here: " + new File("log4j.properties" ).getAbsolutePath());
        p.put( "org.ops4j.pax.logging.property.file",new File("log4j.properties" ).getAbsolutePath());

        return factory.newFramework((Map) p);
    }

    @Override
    public Febo platform(TargetPlatformSpec platform) {
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
