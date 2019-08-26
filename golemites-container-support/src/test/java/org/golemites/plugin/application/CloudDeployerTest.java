package org.golemites.plugin.application;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.golemites.api.Dependency;
import org.golemites.api.GolemitesApplicationExtension;
import org.golemites.api.PushTarget;
import org.golemites.api.TargetPlatformSpec;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.golemites.api.Dependency.dependency;

class CloudDeployerTest {
    @Test
    void tetsBuildImage() throws IOException, InterruptedException, ExecutionException, RegistryException, CacheDirectoryCreationException, InvalidImageReferenceException {
        // JIB does not work nicely with non default FileSystem
        Path root = Files.createTempDirectory(new File("./build/").toPath(),"workdir").toAbsolutePath();
        //Path root = Jimfs.newFileSystem(Configuration.unix()).getPath("/MYTEST");
        // Create test inputs:
        Path inputs = Files.createDirectories(root.resolve("INPUTS"));
        Path p1 = Files.write(inputs.resolve("DATA1"),"data1".getBytes(StandardCharsets.UTF_8));
        Path p2 = Files.write(inputs.resolve("DATA2"),"data2".getBytes(StandardCharsets.UTF_8));
        Path launcher = Files.write(inputs.resolve("LAUNCHER"),"fakeLauncher".getBytes(StandardCharsets.UTF_8));
        Path base = root.resolve("SOMEWHERE/ELSE");
        Files.createDirectories(base);
        ImageBuilder imageBuilder = new ImageBuilder(base);
        TargetPlatformSpec spec = new TargetPlatformSpec();
        spec.setDependencies(new Dependency[] {
                dependency("foo1", p1.toUri()),
                dependency("foo2", p2.toUri())
        });

        TargetPlatformSpec result = imageBuilder.prepare(spec, Arrays.asList());

        GolemitesApplicationExtension config = new GolemitesApplicationExtension();
        config.setPushTo(PushTarget.FILE);
        config.setName(root.getFileName().toString());
        config.setRepository("test");
        CloudDeployer deployer = new CloudDeployer(launcher,base,config);
        String hash = deployer.createImage(root);
        assertThat(hash).isNotBlank();
        // expect a file
        assertThat(base.resolve(config.getName() + "-image.tar.gz")).exists();
    }
}
