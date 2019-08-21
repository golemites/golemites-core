package org.golemites.api;

public interface RepositoryStore
{
    TargetPlatformSpec platform();

    Dependency resolve(String s);
}
