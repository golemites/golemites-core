package org.golemites.repository;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.golemites.repository.EmbeddedStore.prefixWithBase;

public class StoreTest {

    @Test
    void resolveAbsoluteBase() throws URISyntaxException {
        URI input = new URI("file:///PLATFORM/my.jar");
        Path base = Paths.get(new URI("file:///real/base/"));
        URI out = prefixWithBase(base,input);
        assertThat(out.toASCIIString()).isEqualTo("file:///real/base/PLATFORM/my.jar");
    }
}
