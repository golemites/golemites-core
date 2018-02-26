package org.tablerocket.febo.core;

import java.net.URI;

public interface Dependency
{
    String identity();

    URI location();
}
