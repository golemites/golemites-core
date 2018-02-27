package org.tablerocket.febo.api;

import java.net.URI;

public interface Dependency
{
    String identity();

    URI location();
}
