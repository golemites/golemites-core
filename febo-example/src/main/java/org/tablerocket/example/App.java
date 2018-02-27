package org.tablerocket.example;

import com.foo.bar.CompileDependencies;
import com.foo.bar.RepositoryDependencies;

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.tablerocket.febo.core.Febo.febo;

public class App
{
    public static void main(String[] args) throws Exception
    {
        CompileDependencies repo = new CompileDependencies();

        febo()
            .require( repo.dependency(CompileDependencies.ORG_APACHE_FELIX_CONFIGADMIN_1_8_16))
            .require( repo.dependency( RepositoryDependencies.ORG_APACHE_FELIX_SCR_2_0_8 ) )
            .with( "booking", bundle().add( BookingService.class ) )
            .run(args);
    }
}
