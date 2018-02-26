package org.tablerocket.example;

import com.foo.bar.CompileDependencies;
import com.foo.bar.RepositoryDependencies;
import org.tablerocket.febo.core.Repository;
import org.tablerocket.febo.core.Febo;

import java.io.IOException;
import java.util.Properties;

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

public class App
{
    public static void main(String[] args) throws Exception
    {
        // provides access to all required resources available to this febo.
        Repository repo = loadRepo();

        // runline
        try ( Febo febo = new Febo(repo))
        {
            febo.demand( repo.load(CompileDependencies.ORG_APACHE_FELIX_CONFIGADMIN_1_8_16));
            febo.demand( repo.load( RepositoryDependencies.ORG_APACHE_FELIX_SCR_2_0_8 ) );
            febo.install( "booking", bundle().add( BookingService.class ) );
            febo.bounce();
        }
    }

    private static Repository loadRepo() throws IOException
    {
        Properties p = new Properties(  );
        p.load( App.class.getResourceAsStream( "/com.foo.bar.properties" ) );
        return new Repository( p );
    }
}
