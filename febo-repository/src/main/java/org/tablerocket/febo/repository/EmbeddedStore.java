package org.tablerocket.febo.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tablerocket.febo.api.Dependency;
import org.tablerocket.febo.api.RepositoryStore;
import org.tablerocket.febo.api.TargetPlatformSpec;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.tablerocket.febo.repository.ClasspathRepositoryStore.BLOB_FILENAME;

public class EmbeddedStore implements RepositoryStore
{
    private final static Logger LOG = LoggerFactory.getLogger(EmbeddedStore.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final TargetPlatformSpec index;


    public EmbeddedStore()
    {
        this("/" + BLOB_FILENAME);
    }

    public EmbeddedStore(String path )
    {
        try
        {
            index = mapper.readValue(ClasspathRepositoryStore.class.getResourceAsStream( path ), TargetPlatformSpec.class);
            // rewrite embedded resources
            for (Dependency d : index.getDependencies()) {
                URI old = d.getLocation();
                URI newLocation = old; // parseEmbedded(old.toASCIIString());
                LOG.debug("Rewrite location from " + old.toASCIIString() + " to " + newLocation.toASCIIString());
                d.setLocation(newLocation);
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Problem loading store from " + path,e);
        }
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
