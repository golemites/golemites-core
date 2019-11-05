package org.golemites.core;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Helpers {
    private final static Logger LOG = LoggerFactory.getLogger( Helpers.class );

    static void delete(File file) throws IOException {
        if (file.exists() && file.isDirectory()) {
            Path pathToBeDeleted = file.toPath();

            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    static void scan(Bundle b) {
        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream( new URL(b.getLocation()).openStream());

            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && entry.getName().startsWith("OSGI-INF/") && entry.getName().endsWith(".xml")) {
                    LOG.debug(" + " + entry.getName());

                    try (BufferedReader br = new BufferedReader(new InputStreamReader(b.getResource(entry.getName()).openStream()))) {
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            if (LOG.isTraceEnabled()) {
                                System.out.println(line);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
