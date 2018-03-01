package org.tablerocket.febo.repository;

import org.tablerocket.febo.api.Dependency;
import org.tablerocket.febo.core.ResolvedDependency;

import java.io.IOException;
import java.util.Properties;

public abstract class Repository
{
    private final Properties index;

    public Repository(Properties data) {
        this.index = data;
    }

    public Repository() {
        index = new Properties(  );
        try
        {
            index.load( Repository.class.getResourceAsStream( "/febo-blobs.properties" ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Problem loading default props.",e);
        }
    }

    public Dependency dependency(String s)
    {
        return new ResolvedDependency(s,ResolvedDependency.parseLocation( index.getProperty( s )));
    }
}
