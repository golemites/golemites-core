package org.tablerocket.febo.synth;

import okio.BufferedSink;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static okio.Okio.sink;
import static okio.Okio.source;

public class FlatCopyMirror implements Mirror {

    private final File target;
    private static final Logger LOG = LoggerFactory.getLogger(FlatCopyMirror.class);

    public FlatCopyMirror(File target) {
        this.target = target;
    }

    public void mirror(File f) throws IOException {
        try (JarInputStream jip = new JarInputStream(new FileInputStream(f))) {
            JarEntry entry = null;
            while ((entry = jip.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class") && !entry.getName().endsWith("package-info.class")) {
                    LOG.warn(" + " + entry);
                    File out = new File(target,entry.getName());
                    out.getParentFile().mkdirs();
                    try (BufferedSink buffer = Okio.buffer(sink(out))) {
                        buffer.writeAll(source(jip));
                    }
                }
            }
        }
    }
}
