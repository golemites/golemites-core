package org.tablerocket.febo.api;

public interface RepositoryStore
{
    Dependency resolve(String s);

    Dependency[] platform();
}
