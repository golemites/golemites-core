package org.tablerocket.febo.autobundle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class Util
{
    static String convertClassToPath( Class<?> c )
    {
        return c.getName().replace( ".", File.separator ) + ".class";
    }

    public static File findClassesFolder( Class<?> clazz ) throws IOException
    {
        ClassLoader classLoader = clazz.getClassLoader();
        String clazzPath = convertClassToPath( clazz );
        URL url = classLoader.getResource( clazzPath );
        if ( url == null || !"file".equals( url.getProtocol() ) )
        {
            return null;
        }
        else
        {
            try
            {
                File file = new File( url.toURI() );
                String fullPath = file.getCanonicalPath();
                String parentDirPath = fullPath
                    .substring( 0, fullPath.length() - clazzPath.length() );
                return new File( parentDirPath );
            }
            catch ( URISyntaxException e )
            {
                // this should not happen as the uri was obtained from getResource
                throw new RuntimeException( e );
            }
        }
    }
}
