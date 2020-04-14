package org.golemites.launcher.resolver;

import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Super simple karaf feature file parser. Could have used the one from karaf but i feel we don't need the full
 * power here. Also we want to be able to micro-manage deps.
 */
public class FeatureReaderSupport
{
    public static final String ATTR_PREREQUISITE = "prerequisite";
    public static final String ATTR_DEPENDENCY = "dependency";
    private static Logger LOG = LoggerFactory.getLogger( FeatureReaderSupport.class );

    public static final String FEATURE = "feature";
    public static final String BUNDLE = "bundle";
    public static final String DEFAULT_VERSION = "0.0.0";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_VERSION = "version";

    public static List<BundleDependency> listAllBundles(String featureXmlUrl, String feature, String version ) {
        try
        {
            return listAllBundles( new URL( featureXmlUrl ).openStream(),feature,version );
        }
        catch ( IOException e )
        {
            throw new RuntimeException("Check your setup..",e);
        }
    }

    public static List<BundleDependency> listAllBundles( InputStream featureXml, String feature, String version )
    {
        try
        {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse( featureXml );

            // Mark what we are looking for:
            FeatureDependency mainFeature = new FeatureDependency();
            mainFeature.setName( feature );
            mainFeature.setVersion( version );

            // container will keep track of things
            DependencyBuilder builder = new DependencyBuilder( mainFeature );

            // feed the builder
            collect( builder, mainFeature, document.getDocumentElement() );

            return builder.build();
        }catch(Exception e) {
            throw new RuntimeException("Check your setup..",e);
        }
    }

    public static Set<String> listRepos( Source featureXml ) throws IOException {
        try (BufferedSource in = Okio.buffer(featureXml)) {
            return listRepos(in.inputStream());
        }
    }

    public static Set<String> listRepos( InputStream featureXml )
    {
        try
        {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse( featureXml );
            NodeList listOfRepos = document.getDocumentElement().getElementsByTagName("repository");
            Set<String> repos = new HashSet<>();
            for ( int i = 0; i < listOfRepos.getLength(); i++ ) {
                Node repoElement = listOfRepos.item(i);
                repos.add(repoElement.getTextContent().trim());
            }
            return repos;
        }catch(Exception e) {
            throw new RuntimeException("Parsing dependency failed..",e);
        }
    }

    public static void collect( DependencyBuilder builder,FeatureDependency searchFeature, Element start ) throws Exception {
        // First only collect all features we need:
        builder.add( searchFeature );

        NodeList list = start.getElementsByTagName( FEATURE );
        int before = builder.getBundleCount();
        for ( int i = 0; i < list.getLength(); i++ )
        {
            collectFeature( builder,searchFeature, start, list.item( i ) );
        }
        int after = builder.getBundleCount();
        if (before >= after && searchFeature.isPrerequisite()) {
            throw new IllegalArgumentException( "Feature " + searchFeature + " has no bundles. Are you sure it is valid?" );
        }
    }

    private static void collectFeature( DependencyBuilder builder, FeatureDependency searchFeature, Element root,  Node node ) throws Exception
    {
        Node nameAttr = node.getAttributes().getNamedItem( ATTR_NAME );
        Node versionAttr = node.getAttributes().getNamedItem( ATTR_VERSION );

        if ( nameAttr != null )
        {
            String foundFeature = nameAttr.getNodeValue();
            String foundVersion = parseVersion( versionAttr );
            if ( searchFeature.getName().equals( foundFeature ) && matchVersionRange( searchFeature, foundVersion ) )
            {
                FeatureDependency featureDependency = new FeatureDependency();
                featureDependency.setName( foundFeature );
                featureDependency.setVersion( foundVersion );
                builder.add( featureDependency );
                collectBundles( featureDependency,builder, ( Element ) node );
                // go deep!
                NodeList nestedFeatures = ((Element)node).getElementsByTagName( FEATURE );
                for ( int i = 0; i < nestedFeatures.getLength(); i++ ) {
                    Element nestedFeature = (Element)nestedFeatures.item( i );
                    String depVersion = parseVersion( nestedFeature.getAttributes().getNamedItem( ATTR_VERSION ) );


                    String depName = nestedFeature.getTextContent();
                    FeatureDependency dep = new FeatureDependency();
                    dep.setName( depName );
                    dep.setVersion( depVersion );
                    Node preRequisiteAttr = nestedFeature.getAttributes().getNamedItem( ATTR_PREREQUISITE );
                    if (preRequisiteAttr != null) {
                        dep.setPrerequisite( Boolean.parseBoolean( preRequisiteAttr.getNodeValue() ));
                    }
                    Node dependencyAttr = nestedFeature.getAttributes().getNamedItem( ATTR_DEPENDENCY );
                    if (dependencyAttr != null) {
                        dep.setDependency( Boolean.parseBoolean( dependencyAttr.getNodeValue() ));
                    }
                    if (!builder.containsFeature(dep))
                    {
                        collect( builder, dep, root );
                        // so we can detect cycles
                    }else
                    {
                        // cyclic feature dependency. How dare you.
                        // we ignore that for now.

                        throw new IllegalStateException( "CYCLIC feature graph: " + searchFeature + " requires " + dep + " but we already have this." );
                    }
                }
            }
        }
    }

    private static boolean matchVersionRange( FeatureDependency searchFeature, String foundVersion )
    {
        if (searchFeature.getVersion().equals( foundVersion )) {
            return true;
        }else {
            Version v = Version.parseVersion( foundVersion );
            VersionRange range = new VersionRange( searchFeature.getVersion() );
            if (range.includes( v )) {
                return true;
            }
        }
        return false;
    }

    private static String parseVersion( Node versionAttr )
    {
        String foundVersion = DEFAULT_VERSION;
        if (versionAttr != null) {
            foundVersion = versionAttr.getNodeValue();
        }
        return foundVersion;
    }

    private static void collectBundles( FeatureDependency origin, DependencyBuilder builder, Element e )
    {
        NodeList bundles = e.getElementsByTagName( BUNDLE );
        for ( int b = 0; b < bundles.getLength(); b++ )
        {
            Node bundleNode = bundles.item( b );
            String txt = bundleNode.getTextContent();
            BundleDependency dep = new BundleDependency();
            dep.setUrl( txt );
            dep.setSourceFeature( origin );
            builder.add( dep );
        }
    }
}
