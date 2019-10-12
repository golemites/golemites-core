package org.golemites.plugin.application;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.golemites.api.Dependency;
import org.golemites.api.TargetPlatformSpec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.golemites.api.Dependency.dependency;

class ImageBuilderTest {

    @Test
    void testInstallDependenciesFromSpec() throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        // Create test inputs:
        Path inputs = Files.createDirectories(fs.getPath("/").resolve("INPUTS"));
        Path p1 = Files.write(inputs.resolve("DATA1"),"data1".getBytes(StandardCharsets.UTF_8));
        Path p2 = Files.write(inputs.resolve("DATA2"),"data2".getBytes(StandardCharsets.UTF_8));

        // run
        Path base = fs.getPath("/SOMEWHERE/ELSE");
        ImageBuilder imageBuilder = new ImageBuilder(base);
        TargetPlatformSpec spec = new TargetPlatformSpec();
        spec.setDependencies(new Dependency[] {
                dependency("foo1", p1.toUri()),
                dependency("foo2", p2.toUri())
        });

        TargetPlatformSpec result = imageBuilder.prepare(spec);

        Path root = fs.getPath("/");

        // expect stuff in filesystem:
        assertThat(TargetPlatformSpec.platformPath(base).resolve("foo1.jar")).exists();
        assertThat(TargetPlatformSpec.platformPath(base).resolve("foo2.jar")).exists();

        // TODO: make sure we can do "autobundles".

//        assertThat(root.resolve(result.getApplication()[0].getLocation().getPath())).exists();
//        assertThat(root.resolve(result.getApplication()[1].getLocation().getPath())).exists();

        // Constructed pointers in updated spec
        assertThat(spec.getDependencies()[0].getLocation()).isEqualTo(URI.create("file://" + TargetPlatformSpec.platformPath(root) + "/foo1.jar"));
        assertThat(spec.getDependencies()[1].getLocation()).isEqualTo(URI.create("file://" + TargetPlatformSpec.platformPath(root) + "/foo2.jar"));
//        assertThat(spec.getApplication()[0].getLocation()).isEqualTo(URI.create("file:///" + TargetPlatformSpec.applicationPath(root) + "/foo3.jar"));
//        assertThat(spec.getApplication()[1].getLocation()).isEqualTo(URI.create("file:///" + TargetPlatformSpec.applicationPath(root) + "/foo4.jar"));
    }
}
