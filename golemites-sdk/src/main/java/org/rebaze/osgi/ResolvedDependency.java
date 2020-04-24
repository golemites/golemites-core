package org.rebaze.osgi;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@ToString
@EqualsAndHashCode
public class ResolvedDependency implements Dependency
{
    private final String identity;
    private final URI location;
    private Metadata metadata;

    public ResolvedDependency( String key, URI location ) {
        this(key,location,null);
    }

    public ResolvedDependency( String key, URI location, Metadata metadata )
    {
        this.identity = key;
        this.metadata = metadata;
        this.location = location;
    }

    public static URI parseLocation( String location )
    {
        try
        {
            if (location.startsWith( "/" )) {
                return new URI( "file://" + location );

            }else
            {
                return new URI( location );
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

    @Override public Optional<Metadata> metadata()
    {
        return Optional.of(metadata);
    }

    public static class ResolvedMetadata implements Metadata {

        final private String groupId;
        final private String artifactId;
        final private String version;
        final private String classifier;
        final private String type;

        public ResolvedMetadata( String groupId, String artifactId, String version,
            String classifier, String type )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.classifier = classifier;
            this.type = type;
        }

        @Override public String groupId()
        {
            return groupId;
        }

        @Override public String artifactId()
        {
            return artifactId;
        }

        @Override public String version()
        {
            return version;
        }

        @Override public String classifier()
        {
            return classifier;
        }

        @Override public String type()
        {
            return type;
        }
    }
}
