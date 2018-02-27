package org.tablerocket.febo.repository;

import org.tablerocket.febo.api.Dependency;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
        return new ResolvedDependency(s,index.getProperty( s ));
    }

    private class ResolvedDependency implements Dependency
    {
        private final String identity;
        private final URI location;

        public ResolvedDependency( String key, String location )
        {
            this.identity = key;
            try
            {
                if (location.startsWith( "/" )) {
                    this.location = new URI( "file://" + location );

                }else
                {
                    this.location = new URI( location );
                }
            }
            catch ( URISyntaxException e )
            {
                throw new RuntimeException( "Bad url? (got: " + location + ")",e );
            }
        }

        @Override public String identity()
        {
            return identity;
        }

        @Override public URI location()
        {
            return location;
        }
    }
}
