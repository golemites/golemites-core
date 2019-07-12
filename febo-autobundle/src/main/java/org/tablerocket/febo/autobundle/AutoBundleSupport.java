package org.tablerocket.febo.autobundle;

import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.store.Handle;
import org.ops4j.store.Store;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tablerocket.febo.api.DelayedBuilder;
import org.tablerocket.febo.api.Dependency;
import org.tablerocket.febo.api.ResolvedDependency;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

import static org.ops4j.pax.tinybundles.core.TinyBundles.*;
import static org.tablerocket.febo.autobundle.Util.findClassesFolder;

public class AutoBundleSupport
{
    private static final Logger LOG = LoggerFactory.getLogger(AutoBundleSupport.class);

    public AutoBundleSpec from( Class<?> clazz )
    {
        return new AutoBundleSpec( clazz );
    }

    public Set<DelayedBuilder<Dependency>> scan( ClassLoader classLoader )
    {
        // scan classpath
        Set<DelayedBuilder<Dependency>> bundles = new HashSet<>();

        // find all roots:

        List<URL> result = new ArrayList<> ();

        ClassLoader useCl = classLoader;

        while (useCl != null) {
            if (useCl instanceof URLClassLoader) {
                URL[] urls = (( URLClassLoader ) useCl).getURLs();
                result.addAll ( Arrays.asList (urls));
            }
            useCl = useCl.getParent();
        }


        try {
            File base = new File(".").getCanonicalFile().getParentFile();
            LOG.info("Base is " + base.getAbsolutePath());

            // we only care about things that are within our project.
            for (URL url : result) {
                if (url.getProtocol().equals("file")) {

                    File f = new File(url.toURI());
                    if (isPotentialCandidate(base,f)) {
                        LOG.info(" + Candidate for autobundle: " + f.getAbsolutePath());
                        bundles.add(new AutoBundleSpec(f));
                    }else {
                        LOG.debug(" - Not candidate for autobundle: " + f.getAbsolutePath());

                    }
                }
            }
        } catch (URISyntaxException | IOException e) {
            LOG.error("Problem during scanning classpath..",e);
        }
        return bundles;
    }

    private boolean isPotentialCandidate(File base, File f) throws IOException {
        if (f.getAbsolutePath().startsWith(base.getCanonicalPath())) {
            if (f.isDirectory() && f.getAbsolutePath().contains("out/production/classes")) {
                return true;
            } else if (f.isDirectory() && f.getAbsolutePath().contains("/build/classes/java/main")) {
                return true;
            }

            if (f.isFile()) {
                return true;
            }
        }
        return false;
    }

    public static class AutoBundleSpec implements DelayedBuilder<Dependency>
    {
        private final Class<?> clazz;
        private final File root;
        private boolean autoExportApi = true;
        private boolean trimToPrefix = true;

        private Map<String, String> headerBnd = new HashMap<>();

        public AutoBundleSpec( Class<?> clazz )
        {
            this.clazz = clazz;
            this.root = null;
        }

        public AutoBundleSpec( File root )
        {
            this.clazz = null;
            this.root = root;
        }

        public AutoBundleSpec withAutoExportApi( boolean yes )
        {
            this.autoExportApi = yes;
            return this;
        }

        public AutoBundleSpec trimToPrefix( boolean yes )
        {
            this.trimToPrefix = yes;
            return this;
        }

        public AutoBundleSpec withBndHeader( String key, String value )
        {
            this.headerBnd.put( key, value );
            return this;
        }

        public Dependency build()
        {
            try
            {
                Map<String, URL> map = findResources();
                Store<InputStream> store = getDefaultStore();

                if (clazz != null) {
                    String name = clazz.getPackage().getName();
                    String prefix = name.replaceAll("\\.", "/");
                    // then filter out the desired content:


                    List<Map.Entry<String, URL>> list = map.entrySet().stream().filter(
                            entry -> !trimToPrefix || entry.getKey().startsWith(prefix)
                    ).collect(Collectors.toList());

                    TinyBundle bundle = bundle().set(Constants.BUNDLE_SYMBOLICNAME, name);
                    for (Map.Entry<String, URL> entry : list) {
                        bundle.add(entry.getKey(), entry.getValue().openStream());
                    }

                    // Auto Export api packages:
                    if (autoExportApi) {
                        Set<String> exports = new HashSet<>();
                        for (Map.Entry<String, URL> entry : list) {
                            String pack = entry.getKey()
                                    .substring(0, entry.getKey().lastIndexOf("/"))
                                    .replaceAll("/", ".");
                            if (pack.endsWith(".api")) {
                                exports.add(pack);
                            }
                        }
                        if (exports.size() > 0) {
                            bundle.set(Constants.EXPORT_PACKAGE, String.join(",", exports));
                        }
                    }

                    for (Map.Entry<String, String> entry : this.headerBnd.entrySet()) {
                        bundle.set(entry.getKey(), entry.getValue());
                    }

                    Handle handle = store.store(bundle.build(withBnd()));

                    return new ResolvedDependency(name, store.getLocation(handle));
                }else {
                    String name = calculateName(root);

                    List<Map.Entry<String, URL>> list = new ArrayList<>(map.entrySet());

                    TinyBundle bundle = bundle().set(Constants.BUNDLE_SYMBOLICNAME, name);
                    for (Map.Entry<String, URL> entry : list) {
                        bundle.add(entry.getKey(), entry.getValue().openStream());
                    }

                    for (Map.Entry<String, String> entry : this.headerBnd.entrySet()) {
                        bundle.set(entry.getKey(), entry.getValue());
                    }
                    Handle handle = store.store(bundle.build(withBnd()));
                    return new ResolvedDependency(name, store.getLocation(handle));
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }

        private String calculateName(File root) {
            if (root.isDirectory()) {
                // assume gradle:
                if (root.getAbsolutePath().endsWith("/out/production/classes")) {
                    return root.getParentFile().getParentFile().getParentFile().getName();
                } else if (root.getAbsolutePath().endsWith("/build/classes/java/main")) {
                        return root.getParentFile().getParentFile().getParentFile().getParentFile().getName();
                }else {
                    throw new UnsupportedOperationException("Root " + root.getAbsolutePath() + " is unsupported currently.");
                }
            }else {
                String name = root.getName().substring(0, root.getName().length() - 4);
                LOG.info("Root " + root + " becomes " + name);
                return name;
            }
        }

        private Map<String, URL> findResources() throws IOException
        {
            if (clazz != null) {
                ContentCollector collector = selectCollector(clazz);
                Map<String, URL> map = new HashMap<>();
                collector.collect(map);
                return map;
            }else if (root != null && root.isFile()){
                ContentCollector collector = new CollectFromJar(root.toURI().toURL());
                Map<String, URL> map = new HashMap<>();
                collector.collect(map);
                return map;
            }else if (root != null && root.isDirectory()){
                ContentCollector collector = new CollectFromBase(root);
                Map<String, URL> map = new HashMap<>();
                collector.collect(map);
                return map;
            } else {
                throw new IllegalStateException("Must be either based on an anchor class or a valid source jar or directory.");
            }
        }

        private ContentCollector selectCollector( Class<?> anchor ) throws IOException
        {
            File root = findClassesFolder( anchor );

            if ( root != null )
            {
                return new CompositeCollector( new CollectFromBase( root ),
                    new CollectFromItems( anchor ) );
            }
            else
            {
                URL location = anchor.getProtectionDomain().getCodeSource().getLocation();
                return new CollectFromJar(location);
            }
        }

    }
}
