package org.golemites.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.golemites.api.Dependency;
import org.golemites.api.RepositoryStore;
import org.golemites.api.TargetPlatformSpec;

import java.io.IOException;
import java.io.InputStream;

public class ClasspathRepositoryStore implements RepositoryStore
{
    public static final String BLOB_FILENAME = "febo-blobs.json";
    private final TargetPlatformSpec index;
    private static final ObjectMapper mapper = new ObjectMapper();

    public ClasspathRepositoryStore()
    {
        this("/" + BLOB_FILENAME);
    }

    public ClasspathRepositoryStore( String path )
    {
        try
        {
            index = mapper.readValue(ClasspathRepositoryStore.class.getResourceAsStream( path ), TargetPlatformSpec.class);
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Problem loading store from " + path,e);
        }
    }

    public ClasspathRepositoryStore( byte[] data )
    {
        try
        {
            index = mapper.readValue(data, TargetPlatformSpec.class);
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Problem loading store from inputstream",e);
        }
    }

    public ClasspathRepositoryStore( InputStream is )
    {
        try
        {
            index = mapper.readValue(is, TargetPlatformSpec.class);
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Problem loading store from inputstream",e);
        }
    }

    @Override
    public TargetPlatformSpec platform() {
        return index;
    }

    @Override
    public Dependency resolve(String s) {
        throw new UnsupportedOperationException("Cannot resolve from here since it should be all static anyway.");
    }
}
