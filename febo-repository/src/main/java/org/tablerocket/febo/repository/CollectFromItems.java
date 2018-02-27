package org.tablerocket.febo.repository;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class CollectFromItems implements ContentCollector {

    private final List<Class<?>> items;

    public CollectFromItems(Class... items) {
        this.items = Arrays.asList( items );
    }

    public void collect(Map<String, URL> map) throws IOException {
        for (Class<?> s : items) {
            String name = convert(s);
            map.put(name, s.getResource("/" + name));
        }
    }

    private String convert(Class<?> c) {
        return c.getName().replace(".", "/") + ".class";
    }
}
