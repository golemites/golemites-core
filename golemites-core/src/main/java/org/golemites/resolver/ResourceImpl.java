package org.golemites.resolver;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ResourceImpl implements Resource
{
    private final List<Capability> m_caps;
    private final List<Requirement> m_reqs;

    public ResourceImpl() {
        m_caps = new ArrayList<Capability>();
        m_reqs = new ArrayList<Requirement>();
    }
    public ResourceImpl(String name) {
        this(name, IdentityNamespace.TYPE_BUNDLE, Version.emptyVersion);
    }
    public ResourceImpl(String name, String type, Version v)
    {
        m_caps = new ArrayList<Capability>();
        m_caps.add(0, new IdentityCapability(this, name, type, v));
        m_reqs = new ArrayList<Requirement>();
    }

    public void addCapability(Capability cap)
    {
        m_caps.add(cap);
    }

    public List<Capability> getCapabilities(String namespace)
    {
        List<Capability> result = m_caps;
        if (namespace != null)
        {
            result = new ArrayList<Capability>();
            for (Capability cap : m_caps)
            {
                if (cap.getNamespace().equals(namespace))
                {
                    result.add(cap);
                }
            }
        }
        return result;
    }

    public void addRequirement(Requirement req)
    {
        m_reqs.add(req);
    }

    public List<Requirement> getRequirements(String namespace)
    {
        List<Requirement> result = m_reqs;
        if (namespace != null)
        {
            result = new ArrayList<Requirement>();
            for (Requirement req : m_reqs)
            {
                if (req.getNamespace().equals(namespace))
                {
                    result.add(req);
                }
            }
        }
        return result;
    }

    @Override
    public String toString()
    {
        return getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).toString();
    }

    public void addCapabilities(Collection<Capability> caps) {
        for (Capability cap : caps) {
            addCapability(cap);
        }
    }

    public void addRequirements(Collection<Requirement> reqs) {
        for (Requirement req : reqs) {
            addRequirement(req);
        }
    }
}
