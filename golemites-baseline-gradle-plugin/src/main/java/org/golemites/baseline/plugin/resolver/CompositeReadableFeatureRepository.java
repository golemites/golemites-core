package org.golemites.baseline.plugin.resolver;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashSet;
import java.util.Set;

public class CompositeReadableFeatureRepository {

    static Logger LOG = Logging.getLogger(CompositeReadableFeatureRepository.class);
    private Set<FeaturedBundle> index = new HashSet<>();

    public Set<FeaturedBundle> collect(Set<ArtifactDescriptor> repositories) {
        for (ArtifactDescriptor desc : repositories) {
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                        .newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document document = documentBuilder.parse(desc.resolve());
                // feed the builder
                collect(desc,document.getDocumentElement());
            } catch (Exception e) {
                throw new RuntimeException(e);
                //LOG.warn("Potential issue require repository: " + desc + ": " + e.getMessage());
            }
        }
        return index;
    }

    private void collect(ArtifactDescriptor desc, Element documentElement) {
        NodeList bundles = documentElement.getElementsByTagName("bundle");
        int before = index.size();
        for (int b = 0; b < bundles.getLength(); b++) {
            // find the surounding feature:
            FeaturedBundle fb = new FeaturedBundle();
            Feature parent = new Feature();
            fb.setParentFeature(parent);

            Element bundleNode = (Element)bundles.item(b);
            String txt = bundleNode.getTextContent();
            parent.setFeatureRepository(desc);
            fb.setUrl(txt);

            if (bundleNode.hasAttribute("dependency")) {
                parent.setDependency(Boolean.parseBoolean(bundleNode.getAttribute("dependency")));
            }

            Node parentNodeFirst = bundleNode.getParentNode();
            if ("feature".equals(parentNodeFirst.getNodeName())) {
                parseFeatureContextInfoForBundle(parent, (Element) parentNodeFirst);
            }else if ("conditional".equals(parentNodeFirst.getNodeName())) {
                fb.setConditional(true);
                parseFeatureContextInfoForBundle(parent, (Element) bundleNode.getParentNode().getParentNode());
            }else {
                throw new RuntimeException("Cannot parse feature repo: " + desc + " " + parentNodeFirst);
            }

            try {
                LOG.info("+ " + txt + " for feature " + fb.getParentFeature().getName() + " in repo: " + desc.getGroup() + "/" + desc.getName() + "/" + desc.getVersion());
                index.add(fb);
            }catch (Exception e) {
                LOG.warn(e.getMessage());
            }
        }
       // LOG.info("Found in " + desc + " : features: " + features.getLength() + " bundles: " + bundles.getLength() + " effective: " + (index.size() - before));

    }

    private void parseFeatureContextInfoForBundle(Feature parent, Element e) {
        parent.setName(e.getAttribute("name"));
        parent.setVersion(e.getAttribute("version"));
        if (e.hasAttribute("dependency")) {
            parent.setDependency(Boolean.parseBoolean(e.getAttribute("dependency")));
        }
    }
}
