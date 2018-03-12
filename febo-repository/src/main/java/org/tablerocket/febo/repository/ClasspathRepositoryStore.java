package org.tablerocket.febo.repository;

import org.tablerocket.febo.api.Dependency;
import org.tablerocket.febo.core.ResolvedDependency;

import java.io.IOException;
import java.util.Properties;

public class ClasspathRepositoryStore implements RepositoryStore
{
    private final Properties index;

    public Dependency dependency(String s)
    {
        return new ResolvedDependency(s,ResolvedDependency.parseLocation( index.getProperty( s )));
    }

    public ClasspathRepositoryStore()
    {
        this( "/febo-blobs.properties" );
    }

    public ClasspathRepositoryStore( String path )
    {
        index = new Properties(  );
        try
        {
            index.load( ClasspathRepositoryStore.class.getResourceAsStream( path ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Problem loading store from " + path,e);
        }
    }

    @Override public Dependency resolve( String s )
    {
        return new ResolvedDependency(s,ResolvedDependency.parseLocation( index.getProperty( s )));
    }
}
