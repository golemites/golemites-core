package org.tablerocket.febo.testsupport;

import org.junit.jupiter.api.extension.*;
import org.tablerocket.febo.api.Boot;
import org.tablerocket.febo.api.Febo;
import org.tablerocket.febo.autobundle.AutoBundleSupport;
import org.tablerocket.febo.repository.ClasspathRepositoryStore;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class FeboExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback{

    private Map<String, Class<?>> services = new HashMap<>();
    private Febo febo;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        AutoBundleSupport autoBundle = new AutoBundleSupport();
        this.febo = Boot.febo()
                .platform(new ClasspathRepositoryStore().platform()) // the target platform
                .require(autoBundle.discover(getClass().getClassLoader())) // domain bundles
                .keepRunning(true);

        if (context.getTestMethod().isPresent()) {
            Method m = context.getTestMethod().get();
            for (Parameter parameter : m.getParameters()) {
                String name = parameter.getName();
                Class<?> type = parameter.getType();
                services.put(name,type);
                febo.exposePackage(type.getPackage().getName());
            }
        }
        febo.run(new String[]{});
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (febo != null) {
            febo.stop();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Type type = parameterContext.getParameter().getType();
        // TODO: check service availability
        return true;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        try {
            return febo.service(parameterContext.getParameter().getType());
        } catch (Exception e) {
            throw new ParameterResolutionException("Could not aquire service of type " + parameterContext.getParameter().getType().getName());
        }
    }



}
