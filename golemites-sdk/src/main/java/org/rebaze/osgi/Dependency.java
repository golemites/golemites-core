package org.rebaze.osgi;

import java.net.URI;
import java.util.Optional;

public interface Dependency
{
    String identity();

    URI location();

    Optional<Metadata> metadata();

}
