package org.tablerocket.example;

import com.foo.bar.CompileDependencies;
import com.foo.bar.RepositoryDependencies;
import org.tablerocket.febo.core.Repository;

import java.io.IOException;
import java.util.Properties;

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.tablerocket.febo.core.Febo.febo;

public class App
{
    public static void main(String[] args) throws Exception
    {
        Repository repo = loadRepo();

        febo(repo)
            .demand( repo.load(CompileDependencies.ORG_APACHE_FELIX_CONFIGADMIN_1_8_16))
            .demand( repo.load( RepositoryDependencies.ORG_APACHE_FELIX_SCR_2_0_8 ) )
            .install( "booking", bundle().add( BookingService.class ).set( "Export-Package","org.tablerocket.example" ) )
            .run(args);
    }

    private static Repository loadRepo() throws IOException
    {
        Properties p = new Properties(  );
        p.load( App.class.getResourceAsStream( "/com.foo.bar.properties" ) );
        return new Repository( p );
    }
}
