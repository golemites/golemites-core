package org.golemites.autobundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Known to cause some issues. Needs further testing.
 */
@Deprecated
public class CollectFromJar implements ContentCollector {
    private final URL location;
    private static final Logger LOG = LoggerFactory.getLogger(CollectFromJar.class);

    CollectFromJar(URL location) {
        this.location = location;
    }

    @Override
    public void collect(Map<String, URL> map) throws IOException {
        try (JarInputStream jin = new JarInputStream(location.openStream())) {
            JarEntry jentry = null;
            while ((jentry = jin.getNextJarEntry()) != null) {

                String spec = "jar:" + location + "!/" + jentry.getName();

                if (!jentry.isDirectory()) {
                    URL url = new URL(spec);
                    map.put(jentry.getName(), url);
                    LOG.warn(" Adding " + url + " from " + jentry.getLastModifiedTime().toString());
                }
            }
        }

    }
}
