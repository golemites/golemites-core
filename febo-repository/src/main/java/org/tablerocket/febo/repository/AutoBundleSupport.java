package org.tablerocket.febo.repository;

import aQute.lib.strings.Strings;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.store.Handle;
import org.ops4j.store.Store;
import org.osgi.framework.Constants;
import org.tablerocket.febo.api.Dependency;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.getDefaultStore;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

public class AutoBundleSupport
{
    public Dependency from( Class<?> clazz )
    {
        try
        {
            ContentCollector collector = selectCollector(clazz);
            Map<String, URL> map = new HashMap<>();
            collector.collect(map);

            String name = clazz.getPackage().getName();
            String prefix = name.replaceAll("\\.","/");
            // then filter out the desired content:

            List<Map.Entry<String, URL>> list = map.entrySet().stream().filter(
                entry -> entry.getKey().startsWith( prefix )
            ).collect(Collectors.toList());

            TinyBundle bundle = bundle().set(Constants.BUNDLE_SYMBOLICNAME,name );
            for (Map.Entry<String,URL> entry : list) {
                bundle.add(entry.getKey(),entry.getValue().openStream());
            }

            // Auto Export api packages:
            Set<String> exports = new HashSet<>();
            for (Map.Entry<String,URL> entry : list) {
                String pack = entry.getKey().substring( 0,entry.getKey().lastIndexOf( "/" )).replaceAll("/",".");
                if (pack.endsWith( ".api" )) {
                    exports.add(pack);
                }
            }
            if (exports.size() > 0)
            {
                bundle.set( Constants.EXPORT_PACKAGE, String.join( ",", exports ) );
            }
            Store<InputStream> store = getDefaultStore();
            Handle handle = store.store( bundle.build( withBnd() ));

            return new Dependency()
            {
                @Override public String identity()
                {
                    return name;
                }

                @Override public URI location()
                {
                    try
                    {
                        return store.getLocation( handle );
                    }
                    catch ( IOException e )
                    {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
        catch ( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    private ContentCollector selectCollector(Class<?> anchor) throws IOException {
        File root = findClassesFolder(anchor);

        if (root != null) {
            return new CompositeCollector(new CollectFromBase(root), new CollectFromItems(anchor));
        }
        else {
            return new CollectFromItems(anchor);
        }
    }

    static String convertClassToPath(Class<?> c) {
        return c.getName().replace(".", File.separator) + ".class";
    }


    public static File findClassesFolder(Class<?> clazz) throws IOException
    {
        ClassLoader classLoader = clazz.getClassLoader();
        String clazzPath = convertClassToPath(clazz);
        URL url = classLoader.getResource(clazzPath);
        if (url == null || !"file".equals(url.getProtocol())) {
            return null;
        }
        else {
            try {
                File file = new File(url.toURI());
                String fullPath = file.getCanonicalPath();
                String parentDirPath = fullPath
                    .substring(0, fullPath.length() - clazzPath.length());
                return new File(parentDirPath);
            }
            catch (URISyntaxException e) {
                // this should not happen as the uri was obtained from getResource
                throw new RuntimeException(e);
            }
        }
    }
}
