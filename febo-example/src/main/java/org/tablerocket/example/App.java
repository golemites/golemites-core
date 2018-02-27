package org.tablerocket.example;

import com.foo.bar.FeboRepository;

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.tablerocket.febo.core.Febo.febo;

public class App
{
    public static void main(String[] args) throws Exception
    {
        FeboRepository repo = new FeboRepository();

        febo()
            .require( repo.org_apache_felix_configadmin())
            .require( repo.org_apache_felix_scr() )
            .with( "booking", bundle().add( BookingService.class ) )
            .run(args);
    }
}
