package org.golemites.launcher;

import org.golemites.api.Boot;
import org.golemites.api.ModuleRuntime;
import org.golemites.api.Entrypoint;
import org.golemites.repository.EmbeddedStore;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class Launcher {
    private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

    @Option( name = "--workingDirectory", usage = "working directory", metaVar = "WORKDIR" )
    private File workdir = new File( "." );

    @Option( name = "--debug", usage = "show additional debug info" )
    private boolean debug = false;

    @Argument( index = 0, required = true, multiValued = true, metaVar = "COMMAND", usage = "command to execute" )
    private String[] command;

    public static void main( String[] args )
    {
        Launcher main = new Launcher();
        ParserProperties parserProperties = ParserProperties.defaults().withUsageWidth( 120 );

        CmdLineParser parser = new CmdLineParser( main, parserProperties );
        try
        {
            // prepend the defaults if they exist:
            args = addEnvironmentDefaults(args);
            // parse the arguments.
            parser.parseArgument( args );
            configureLogging(main.debug);
            if ( main.command == null )
            {
                printHelp( parser );
            }
            else
            {
                main.execute();
            }
        }
        catch ( CmdLineException e )
        {
            configureLogging(main.debug);
            printHelp( parser );
        }
        catch ( Exception e )
        {
            Logger log = LoggerFactory.getLogger(Launcher.class);
            CliUtil.printFailure( e.getMessage() );
            if ( main.debug )
            {
                log.error("Problem executing command.",e);
            }
        }
    }

    private void bootOsgi() throws Exception {
        // this needs to have platform + application bundles in blob.
        ModuleRuntime moduleRuntime = Boot.findModuleRuntime(this.getClass().getClassLoader()).platform(new EmbeddedStore().platform());

        moduleRuntime.start();
        Optional<Entrypoint> command = moduleRuntime.service(Entrypoint.class);
        if (command.isPresent()) {
            LOG.info("Found command, executing..");
            command.get().execute(this.command,System.in, System.out,System.err);
            // hard exit
            System.exit(0);

            moduleRuntime.stop();
        }else {
            LOG.info("Golemites is running in server mode.");
        }
    }

    private void execute() throws Exception {
        printLogo();
        bootOsgi();
    }

    private static void configureLogging(boolean debug) {
//        if (!debug) {
//            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback-default.xml");
//        }else {
//            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback-debug.xml");
//        }
    }

    private static String[] addEnvironmentDefaults( String[] args )
    {
        File env = new File(".environment");
        if (env.exists()) {
            Properties p = new Properties();
            try(FileInputStream inp = new FileInputStream( env ))
            {
                p.load( inp );
                // create additions:
                List<String> appendix = new ArrayList<>(  );
                for (String key : p.stringPropertyNames()) {
                    String value = p.getProperty( key );
                    if (value != null) {
                        appendix.add( "--"+key );
                        appendix.add(value );
                    }
                }
                String[] extra = appendix.toArray( new String[0] );
                // ultimately combine them:
                args = Stream.concat( stream(extra),stream(args)).toArray( String[]::new );
            }
            catch ( IOException e )
            {
                // ignore.
                e.printStackTrace();
            }
        }
        return args;
    }

    private static void printHelp( CmdLineParser parser )
    {
        printLogo();
        System.err.println( "golem [options...] COMMAND" );
        System.err.println( "" );
        System.err.println( "Available Commands " );
        System.err.println( "==================================" );
        System.err.println( "golem run           : Build and Run application in the cloud" );
        System.err.println( "" );
        System.err.println( "Options" );
        System.err.println( "==================================" );
        parser.printUsage( System.err );
    }

    private static void printLogo() {


        String logo3 = "                                       \n" +
                "                &@   @#                \n" +
                "             @@@@@@ @@@@@@             \n" +
                "         #@@@@@@@@@ @@@@@@@@@/         \n" +
                "      @@@@@@@@@@@@@ @@@@@@@@@@@@@      \n" +
                "    @@@@@@@@@@@@@@# @@@@@@@@@@@@@@@    \n" +
                "    @@@@@@@@@@@%       @@@@@@@@@@@@    \n" +
                "    @@@@@@@@              *@@@@@@@@    \n" +
                "    @@@@@@@@               @@@@@@@@    \n" +
                "    @@@@@@@@               @@@@@@@@    \n" +
                "    @@@@@@@@               @@@@@@@@    \n" +
                "    @@@@@@@                @@@@@@@@    \n" +
                "    @@@/  /@@@#         &@@@@@@@@@@    \n" +
                "       @@@@@@@@@@@   @@@@@@@@@@@@@@    \n" +
                "      @@@@@@@@@@@@@@@@@@@@@@@@@@@@     \n" +
                "        %@@@@@@@@@@@@@@@@@@@@@(        \n" +
                "            @@@@@@@@@@@@@@@            \n" +
                "               &@@@@@@@%            ";
        String logo4 = "                                                           \n" +
                "                                                           \n" +
                "                        @@@@   @@@@                        \n" +
                "                    *@@@@@@@   @@@@@@@*                    \n" +
                "                 @@@@@@@@@@@   @@@@@@@@@@@                 \n" +
                "             ,@@@@@@@@@@@@@@   @@@@@@@@@@@@@@,             \n" +
                "          &@@@@@@@@@@@@@@@@@   @@@@@@@@@@@@@@@@@&          \n" +
                "       @@@@@@@@@@@@@@@@@@@@@   @@@@@@@@@@@@@@@@@@@@@       \n" +
                "      @@@@@@@@@@@@@@@@@@@@@@   @@@@@@@@@@@@@@@@@@@@@@      \n" +
                "      @@@@@@@@@@@@@@@@@@&         &@@@@@@@@@@@@@@@@@@      \n" +
                "      @@@@@@@@@@@@@@@*               *@@@@@@@@@@@@@@@      \n" +
                "      @@@@@@@@@@@@&                     &@@@@@@@@@@@@      \n" +
                "      @@@@@@@@@@@@*                     *@@@@@@@@@@@@      \n" +
                "      @@@@@@@@@@@@*                     *@@@@@@@@@@@@      \n" +
                "      @@@@@@@@@@@@*                     *@@@@@@@@@@@@      \n" +
                "      @@@@@@@@@@@@*                     *@@@@@@@@@@@@      \n" +
                "      @@@@@@@@@@@@                      *@@@@@@@@@@@@      \n" +
                "      @@@@@@@@@@                        &@@@@@@@@@@@@      \n" +
                "      @@@@@@#    @@@@                 @@@@@@@@@@@@@@@      \n" +
                "       #&    *@@@@@@@@@@&         &@@@@@@@@@@@@@@@@@@      \n" +
                "          %@@@@@@@@@@@@@@@@@   @@@@@@@@@@@@@@@@@@@@@@      \n" +
                "         @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@       \n" +
                "          &@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@&          \n" +
                "             ,@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@,             \n" +
                "                 @@@@@@@@@@@@@@@@@@@@@@@@@                 \n" +
                "                    *@@@@@@@@@@@@@@@@@*                    \n" +
                "                        @@@@@@@@@@@                        \n" +
                "                           (@@@(                           \n" +
                "                                                       ";

        CliUtil.printInfo("GOLEMITES controller is starting..");
        System.out.println(logo4);
    }
}
