package org.tablerocket.febo.api;

public interface RepositoryStore
{
    TargetPlatformSpec platform();

    Dependency resolve(String s);
}
