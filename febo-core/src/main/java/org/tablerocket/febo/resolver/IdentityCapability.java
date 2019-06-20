package org.tablerocket.febo.resolver;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import java.util.HashMap;
import java.util.Map;

public class IdentityCapability implements Capability
{
    private final Resource m_resource;
    private final Map<String, String> m_dirs;
    private final Map<String, Object> m_attrs;

    public IdentityCapability(Resource resource, String name, String type, Version v)
    {
        m_resource = resource;
        m_dirs = new HashMap<String, String>();
        m_attrs = new HashMap<String, Object>();
        m_attrs.put(IdentityNamespace.IDENTITY_NAMESPACE, name);
        m_attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, type);
        m_attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, v);
    }

    public String getNamespace()
    {
        return IdentityNamespace.IDENTITY_NAMESPACE;
    }

    public Map<String, String> getDirectives()
    {
        return m_dirs;
    }

    public Map<String, Object> getAttributes()
    {
        return m_attrs;
    }

    public Resource getResource()
    {
        return m_resource;
    }

    @Override
    public String toString()
    {
        return getNamespace() + "; "
                + getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString();
    }
}
