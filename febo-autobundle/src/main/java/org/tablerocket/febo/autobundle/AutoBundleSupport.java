package org.tablerocket.febo.autobundle;

import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.store.Handle;
import org.ops4j.store.Store;
import org.osgi.framework.Constants;
import org.tablerocket.febo.api.DelayedBuilder;
import org.tablerocket.febo.api.Dependency;
import org.tablerocket.febo.core.ResolvedDependency;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.ops4j.pax.tinybundles.core.TinyBundles.*;
import static org.tablerocket.febo.autobundle.Util.findClassesFolder;

public class AutoBundleSupport
{
    public AutoBundleSpec from( Class<?> clazz )
    {
        return new AutoBundleSpec( clazz );
    }

    public static class AutoBundleSpec implements DelayedBuilder<Dependency>
    {
        private final Class<?> clazz;
        private final Set<File> roots;
        private boolean autoExportApi = true;
        private boolean trimToPrefix = true;

        private Map<String, String> headerBnd = new HashMap<>();

        public AutoBundleSpec( Class<?> clazz )
        {
            this.clazz = clazz;
            this.roots = null;
            // headerBnd.put("-dsannotations-options","version;minimum=1.4.0");
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

                String name = clazz.getPackage().getName();
                String prefix = name.replaceAll( "\\.", "/" );
                // then filter out the desired content:


                List<Map.Entry<String, URL>> list = map.entrySet().stream().filter(
                    entry -> !trimToPrefix || entry.getKey().startsWith( prefix )
                ).collect( Collectors.toList() );

                TinyBundle bundle = bundle().set( Constants.BUNDLE_SYMBOLICNAME, name );
                for ( Map.Entry<String, URL> entry : list )
                {
                    bundle.add( entry.getKey(), entry.getValue().openStream() );
                }

                // Auto Export api packages:
                if ( autoExportApi )
                {
                    Set<String> exports = new HashSet<>();
                    for ( Map.Entry<String, URL> entry : list )
                    {
                        String pack = entry.getKey()
                            .substring( 0, entry.getKey().lastIndexOf( "/" ) )
                            .replaceAll( "/", "." );
                        if ( pack.endsWith( ".api" ) )
                        {
                            exports.add( pack );
                        }
                    }
                    if ( exports.size() > 0 )
                    {
                        bundle.set( Constants.EXPORT_PACKAGE, String.join( ",", exports ) );
                    }
                }

                for ( Map.Entry<String, String> entry : this.headerBnd.entrySet() )
                {
                    bundle.set( entry.getKey(), entry.getValue() );
                }

                Store<InputStream> store = getDefaultStore();
                Handle handle = store.store( bundle.build( withBnd() ) );

                return new ResolvedDependency(name,store.getLocation( handle ));
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }

        private Map<String, URL> findResources() throws IOException
        {
            ContentCollector collector = selectCollector( clazz );
            Map<String, URL> map = new HashMap<>();
            collector.collect( map );
            return map;
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
                return new CollectFromItems( anchor );
            }
        }

    }
}
