package org.tablerocket.febo.repository;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tablerocket.febo.api.Dependency;
import org.tablerocket.febo.api.RepositoryStore;
import org.tablerocket.febo.api.TargetPlatformSpec;

import java.io.IOException;
import java.io.InputStream;

public class ClasspathRepositoryStore implements RepositoryStore
{
    public static final String BLOB_FILENAME = "febo-blobs.json";
    private final TargetPlatformSpec index;

    public ClasspathRepositoryStore()
    {
        this("/" + BLOB_FILENAME);
    }

    public ClasspathRepositoryStore( String path )
    {
        ObjectMapper mapper = new ObjectMapper();
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
        ObjectMapper mapper = new ObjectMapper();
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
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
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
        return null;
    }
}
