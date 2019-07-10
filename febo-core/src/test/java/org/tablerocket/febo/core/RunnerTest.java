package org.tablerocket.febo.core;

import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.*;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;
import org.tablerocket.febo.resolver.PackageCapability;
import org.tablerocket.febo.resolver.PackageRequirement;
import org.tablerocket.febo.resolver.ResolverContextImpl;
import org.tablerocket.febo.resolver.ResourceImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunnerTest
{
    @Test
    @Disabled
    void resolverTest() throws ResolutionException {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<>();
        List<Resource> mandatory = populate(wirings, candMap);
        ResolverContextImpl rci = new ResolverContextImpl(wirings, candMap, mandatory, Collections.emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);
        assertEquals(2, wireMap.size());

        Resource aRes = findResource("A", wireMap.keySet());
        List<Wire> aWires = wireMap.get(aRes);
        assertEquals(0, aWires.size());

        Resource bRes = findResource("B", wireMap.keySet());
        List<Wire> bWires = wireMap.get(bRes);
        assertEquals(1, bWires.size());
        Wire bWire = bWires.iterator().next();
        assertEquals(aRes, bWire.getProvider());
        assertEquals(bRes, bWire.getRequirer());
        Capability cap = bWire.getCapability();
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE, cap.getNamespace());
        assertEquals(1, cap.getAttributes().size());
        assertEquals("foo", cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
        assertEquals(0, cap.getDirectives().size());
        assertEquals(aRes, cap.getResource());

        Requirement req = bWire.getRequirement();
        assertEquals(1, req.getDirectives().size());
        assertEquals("(osgi.wiring.package=foo)", req.getDirectives().get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE));
        assertEquals(0, req.getAttributes().size());
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE, req.getNamespace());
        assertEquals(bRes, req.getResource());
    }

    private static List<Resource> populate(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        ResourceImpl exporter = new ResourceImpl("A");
        exporter.addCapability(new PackageCapability(exporter, "foo"));
        ResourceImpl importer = new ResourceImpl("B");
        importer.addRequirement(new PackageRequirement(importer, "foo"));
        //candMap.put(importer.getRequirements(null).get(0), exporter.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE));
        List<Resource> resources = new ArrayList<>();
        resources.add(importer);
        return resources;
    }

    private static String getResourceName(Resource r)
    {
        return r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes()
                .get(IdentityNamespace.IDENTITY_NAMESPACE).toString();
    }

    private static Resource findResource(String identity, Collection<Resource> resources)
    {
        for (Resource r : resources)
        {
            if (identity.equals(getResourceName(r)))
                return r;
        }
        return null;
    }
}
