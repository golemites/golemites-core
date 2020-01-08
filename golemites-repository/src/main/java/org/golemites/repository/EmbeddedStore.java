package org.golemites.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.golemites.api.Dependency;
import org.golemites.api.RepositoryStore;
import org.golemites.api.TargetPlatformSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.golemites.repository.ClasspathRepositoryStore.BLOB_FILENAME;

public class EmbeddedStore implements RepositoryStore
{
    private final static Logger LOG = LoggerFactory.getLogger(EmbeddedStore.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final TargetPlatformSpec index;

    public EmbeddedStore()
    {
        this( new File(".").toPath());
    }

    public EmbeddedStore(Path base)
    {
        this( base, BLOB_FILENAME);
    }

    private EmbeddedStore(Path base, String filename)
    {
        String path = "/" + filename;
        try
        {
            Path descriptor = base.resolve("CONFIGURATION").resolve(filename);
            if (descriptor.toFile().exists()) {
                index = mapper.readValue(descriptor.toFile(), TargetPlatformSpec.class);
            }else {
                if (ClasspathRepositoryStore.class.getResource(path) != null) {
                    index = mapper.readValue(ClasspathRepositoryStore.class.getResourceAsStream(path), TargetPlatformSpec.class);
                }else {
                    throw new RuntimeException("No Platform found to be used.");
                }

            }
            // rewrite dependency actual location
            for (Dependency d : index.getDependencies()) {
                URI givenLocation = d.getLocation();
                URI externalLocation = prefixWithBase(base,givenLocation);
                URI embeddedLocation = parseEmbedded(d.getLocation());
                // Now which wins? Actually it does not really matter when using hashes as the final classifier.
                // Therefor we just try all in a defined order
                if (checkLocationHasContent("given",givenLocation)) {
                    // unlikely.
                    LOG.debug("Location exists as is: " + givenLocation.toASCIIString());
                    d.setLocation(externalLocation);
                }else if (checkLocationHasContent("embeddedLocation",embeddedLocation)) {
                    // in embedded standalone runner case.
                    LOG.debug("Rewrite location from " + givenLocation.toASCIIString() + " to " + embeddedLocation.toASCIIString());
                    d.setLocation(embeddedLocation);
                }else if (checkLocationHasContent("externalLocation",externalLocation)) {
                    // exploded case. Either using containers or local debugging.
                    LOG.debug("Rewrite location from " + givenLocation.toASCIIString() + " to " + externalLocation.toASCIIString());
                    d.setLocation(externalLocation);
                }else {
                    throw new RuntimeException("Problem loading dependency " + d);
                }
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Problem loading store from " + path,e);
        }
    }

    private boolean checkLocationHasContent(String name, URI uri) {
        try {
            URL url = uri.toURL();
            URLConnection is = url.openConnection();
            is.connect();
        }catch(Exception e) {
            LOG.info(" - Connecting to " + uri.toASCIIString() + " as " + name + " failed with exception: " + e.getMessage());
            return false;
        }
        LOG.info(" + Connecting to " + uri.toASCIIString() + " as " + name + " succeeded.");
        return true;
    }

    public static URI prefixWithBase(Path base, URI givenLocation) {
        Path given = Paths.get(givenLocation);
        return base.resolve(Paths.get("/").relativize(given)).normalize().toUri();
    }
    private static URI parseEmbedded(URI location )
    {
        try
        {
            URL parent = Dependency.class.getProtectionDomain().getCodeSource().getLocation();
            return new URI("jar:" + parent.toExternalForm() + "!/" + location.getPath());
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( "Bad url? (got: " + location + ")",e );
        }
    }

    @Override
    public TargetPlatformSpec platform() {
        return index;
    }

    @Override
    public Dependency resolve(String s) {
        throw new UnsupportedOperationException("Custom resolve is not supported anymore.");
    }
}
