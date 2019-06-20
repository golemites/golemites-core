package org.tablerocket.febo.resolver;

import org.osgi.resource.*;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import java.util.*;

public class ResolverContextImpl extends ResolveContext
{
    private final Map<Resource, Wiring> m_wirings;
    private final Map<Requirement, List<Capability>> m_candMap;
    private final Collection<Resource> m_mandatory;
    private final Collection<Resource> m_optional;

    public ResolverContextImpl(
            Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap,
            Collection<Resource> mandatory, Collection<Resource> optional)
    {
        m_wirings = wirings;
        m_candMap = candMap;
        m_mandatory = mandatory;
        m_optional = optional;
    }

    @Override
    public Collection<Resource> getMandatoryResources()
    {
        return new ArrayList<Resource>(m_mandatory);
    }

    @Override
    public Collection<Resource> getOptionalResources()
    {
        return new ArrayList<Resource>(m_optional);
    }

    @Override
    public List<Capability> findProviders(Requirement r)
    {
        List<Capability> cs = m_candMap.get(r);
        if (cs != null) {
            return new ArrayList<Capability>(cs);
        } else {
            return new ArrayList<Capability>();
        }
    }

    @Override
    public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability)
    {
        int idx = 0;
        capabilities.add(idx, hostedCapability);
        return idx;
    }

    @Override
    public boolean isEffective(Requirement requirement)
    {
        return true;
    }

    @Override
    public Map<Resource, Wiring> getWirings()
    {
        return m_wirings;
    }

    public static class FelixResolveContextImpl extends ResolverContextImpl
    {
        private final Map<Wiring, List<Wire>> m_substitutions;

        public FelixResolveContextImpl(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap, Collection<Resource> mandatory, Collection<Resource> optional, Map<Wiring, List<Wire>> substitutions)
        {
            super(wirings, candMap, mandatory, optional);
            this.m_substitutions = substitutions;
        }

        public Collection<Resource> getOndemandResources(Resource host)
        {
            return Collections.emptyList();
        }

        public List<Wire> getSubstitutionWires(Wiring wiring)
        {
            List<Wire> result = m_substitutions.get(wiring);
            return result == null ? Collections.<Wire> emptyList() : result;
        }

    }
}