package org.golemites.testsupport;

import org.golemites.api.Dependency;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.TargetPlatformSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.golemites.testsupport.GolemitesExtension.createAndGetPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GolemitesTCK {
    private static Logger LOG = LoggerFactory.getLogger(GolemitesTCK.class);

    @TestFactory
    Collection<DynamicNode> tckFactory() throws IOException {
        TargetPlatformSpec p = createAndGetPlatform();
        List<DynamicNode> all = new ArrayList<>();
        List<DynamicTest> osgiBundleTests = new ArrayList<>();
        for (Dependency d : p.getDependencies()) {
            osgiBundleTests.add(DynamicTest.dynamicTest("Is OSGi Bundle: " + d.getMetadata().getGroupId() + ":" + d.getMetadata().getArtifactId() + ":" + d.getMetadata().getVersion(), isOsgiBundle(d)));
        }
        DynamicNode node = DynamicContainer.dynamicContainer("OSGi Bundles",osgiBundleTests);
        all.add(node);
        return all;
    }

    private Executable isOsgiBundle(Dependency d) {
        return () -> {
            System.out.println("Verifying " + d.getMetadata().getBundleSymbolicName());
            Assertions.assertNotNull(d.getMetadata().getBundleSymbolicName(),"Bundle Symbolic Name");

        };
    }
}
