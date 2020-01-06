package org.golemites.testsupport;

import org.golemites.api.Boot;
import org.golemites.api.ModuleRuntime;
import org.golemites.repository.ClasspathRepositoryStore;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.golemites.repository.ClasspathRepositoryStore.BLOB_FILENAME;

public class GolemitesExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback, BeforeAllCallback {

    private Map<String, Class<?>> services = new HashMap<>();
    private ModuleRuntime moduleRuntime;
    private Logger LOG = LoggerFactory.getLogger(GolemitesExtension.class);
    private File blob;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // make sure project "plan" is available:
        // find out if we are currently running in a gradle build and infer resolution instead of this.

        File base = new File(".");
        LOG.warn("Creating plan from folder: " + base.getAbsoluteFile().getCanonicalPath());
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(base)
                .connect();
        GradleProject p = connection.getModel(GradleProject.class);
        GradleTask gestaltTask = findGestalt(p);

        if (gestaltTask == null) {
            throw new IllegalArgumentException("Gestalt Project is not set up.");
        } else {
            LOG.warn("Found Gestalt Task to be executed: " + gestaltTask);
        }

        try {
            connection.newBuild().forTasks(gestaltTask).run(new ResultHandler<Void>() {
                @Override
                public void onComplete(Void result) {
                    LOG.warn("Gestalt build ran successfully.");
                }

                @Override
                public void onFailure(GradleConnectionException failure) {
                    LOG.error("Gestalt build ran with errors.", failure);

                }
            });
        } finally {
            connection.close();
        }
        // Here we should find the blob:
        blob = new File(gestaltTask.getProject().getBuildDirectory(), "generated/resources/" + BLOB_FILENAME);

        LOG.warn("Done: " + blob);

    }

    private GradleTask findGestalt(GradleProject p) {
        for (GradleTask task : p.getTasks()) {
            if ("gestalt".equals(task.getName())) {
                return task;
            }
        }
        for (GradleProject sub : p.getChildren()) {
            return findGestalt(sub);
        }
        return null;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        this.moduleRuntime = Boot.findModuleRuntime(this.getClass().getClassLoader())
                .platform(new ClasspathRepositoryStore(new FileInputStream(blob)).platform());

        if (context.getTestMethod().isPresent()) {
            Method m = context.getTestMethod().get();
            for (Parameter parameter : m.getParameters()) {
                String name = parameter.getName();
                Class<?> type = parameter.getType();
                expose(name, type);
            }
        }
        moduleRuntime.start();
    }

    private void expose(String name, Class<?> type) {
        services.put(name, type);
        moduleRuntime.exposePackage(type.getPackage().getName());
        for (Method m : type.getMethods()) {
            if (m.getReturnType() != null && m.getReturnType().getPackage() != null) {
                moduleRuntime.exposePackage(m.getReturnType().getPackage().getName());
            }
            for (Parameter p : m.getParameters()) {
                if (p.getType() != null && p.getType().getPackage() != null) {
                    moduleRuntime.exposePackage(p.getType().getPackage().getName());
                }
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (moduleRuntime != null) {
            moduleRuntime.stop();
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
            Optional<Object> s = (Optional<Object>) moduleRuntime.service(parameterContext.getParameter().getType());
            if (s.isPresent()) {
                return s.get();
            } else {
                throw new ParameterResolutionException("Service " + parameterContext.getParameter().getType() + " did not get resolved.");
            }
        } catch (Exception e) {
            throw new ParameterResolutionException("Could not aquire service of type " + parameterContext.getParameter().getType().getName());
        }
    }

}
