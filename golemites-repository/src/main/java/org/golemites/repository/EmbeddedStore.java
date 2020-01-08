package org.golemites.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.golemites.api.Dependency;
import org.golemites.api.RepositoryStore;
import org.golemites.api.TargetPlatformSpec;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
            // rewrite embedded resources
            for (Dependency d : index.getDependencies()) {
                URI givenLocation = d.getLocation();
                // URI newLocation = old; // parseEmbedded(old.toASCIIString());
                URI newLocation = prefixWithBase(base,givenLocation); // parseEmbedded(old.toASCIIString());
                LOG.debug("Rewrite location from " + givenLocation.toASCIIString() + " to " + newLocation.toASCIIString());
                d.setLocation(newLocation);
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Problem loading store from " + path,e);
        }
    }

    static URI prefixWithBase(Path base, URI givenLocation) {
        Path given = Paths.get(givenLocation);
        return base.resolve(Paths.get("/").relativize(given)).normalize().toUri();
    }

    private static URI parseEmbedded(String location )
    {
        try
        {
            URL parent = Dependency.class.getProtectionDomain().getCodeSource().getLocation();
            return new URI("jar:" + parent.toExternalForm() + "!/" + location);
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
