package org.golemites.autobundle;

import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.store.Handle;
import org.ops4j.store.Store;
import org.golemites.api.DelayedBuilder;
import org.golemites.api.Dependency;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.ops4j.pax.tinybundles.core.TinyBundles.*;

@Deprecated
public class BndProject implements DelayedBuilder<Dependency>
{
    private String name;

    public BndProject from( String name )
    {
        this.name = name;
        return this;
    }

    @Override public Dependency build()
    {
        Properties p = new Properties();
        try
        {
            // All this will be discovered from gradle model later. Static for now.
            File parent = new File(".");
            File bndFile = new File( parent, name + "/bnd.bnd" );
            if (!bndFile.exists()) throw new RuntimeException( "There is no bnd.bnd at " + bndFile.getAbsolutePath() );
            p.load( new FileInputStream( bndFile ));

            Set<File> roots = new HashSet<>(  );
            //roots.add(new File(parent, name + "/out/production/classes"));
            roots.add(new File(parent, name + "/build/classes/java/main"));

            Map<String, URL> map = findResources(roots);

            List<Map.Entry<String, URL>> list = new ArrayList<>( map.entrySet() );

            TinyBundle bundle = bundle();
            for (Object key : p.keySet()) {
                bundle.set( (String)key,p.getProperty( ( String ) key ) );
            }

            for ( Map.Entry<String, URL> entry : list )
            {
                bundle.add( entry.getKey(), entry.getValue().openStream() );
            }

            Store<InputStream> store = getDefaultStore();
            Handle handle = store.store( bundle.build( withBnd() ) );

            Dependency d =  new Dependency();
            d.setIdentity(name);
            d.setLocation(store.getLocation( handle ));
            return d;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private Map<String, URL> findResources(Set<File> roots) throws IOException
    {
        ContentCollector collector = selectCollector( roots );
        Map<String, URL> map = new HashMap<>();
        collector.collect( map );
        return map;
    }

    private ContentCollector selectCollector( Set<File> roots ) throws IOException
    {
        Set<ContentCollector> collectors = new HashSet<>(  );
        for (File root : roots) {
            if (root.exists() && root.isDirectory())
            {
                collectors.add( new CollectFromBase( root ) );
            }
        }
        return new CompositeCollector( collectors.toArray( new ContentCollector[roots.size()] ) );
    }
}
