package org.tablerocket.febo.repository;

import org.tablerocket.febo.api.Dependency;

public interface RepositoryStore
{
    Dependency resolve(String s);
}
