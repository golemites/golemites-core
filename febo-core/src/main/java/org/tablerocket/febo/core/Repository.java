package org.tablerocket.febo.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class Repository
{
    private final Properties index;

    public Repository(Properties data) {
        this.index = data;
    }

    public Dependency load(String s)
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
