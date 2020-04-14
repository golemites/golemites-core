package org.golemites.launcher.resolver;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static okio.Okio.buffer;

public class FeatureRepositoryResolverTask {

    public static final String INPUT_CONFIG = "toni";

    public Set<ArtifactDescriptor> loadArtifactsTransitively() throws Exception {
        Set<ArtifactDescriptor> givenRepos = findTransitiveKaraFeatureRepositories(featureRepo);
        Set<FeaturedBundle> bundleRefs = new CompositeReadableFeatureRepository().collect(givenRepos);

        // convert them to artifacts (aka parsing):
        Set<ArtifactDescriptor> resolvedBundles = bundleRefs.stream()
                .map((s) -> parseGAV(s.url, this::resolveSingle))
                .collect(Collectors.toSet());
        resolvedBundles.addAll(givenRepos);

        return resolvedBundles; //materialize(resolvedBundles,target);
    }

    private Set<ArtifactDescriptor> findTransitiveKaraFeatureRepositories() throws IOException {
        Set<ArtifactDescriptor> input = null; // INPUT!!!
        Set<ArtifactDescriptor> newRepos = null //; findEmbeddedRepos();

        findEmbedded(givenRepos, newRepos);
        return givenRepos;
    }

    private Set<Source> resolveAll(Configuration featureRepo) throws FileNotFoundException {
        Set<File> resolved = featureRepo.resolve();
        Set<Source> builtIn = new HashSet<>();
        for (File f : resolved) {
            builtIn.add(Okio.source(f));
        }
        return builtIn;
    }


    private void findEmbedded(Set<ArtifactDescriptor> givenRepos, Set<ArtifactDescriptor> newRepos) throws IOException {
        for (ArtifactDescriptor desc : newRepos) {
            if (!givenRepos.contains(desc)) {
                givenRepos.add(desc);
                findEmbedded(givenRepos, findEmbeddedRepos(Okio.source(resolveSingle(desc))));
            }
        }
    }

    private Set<ArtifactDescriptor> findEmbeddedRepos(Source... loaded) throws IOException {
        Set<ArtifactDescriptor> newRepos = new HashSet<>();
        for (Source src : loaded) {
            Set<String> inRep = FeatureReaderSupport.listRepos(src);
            for (String r : inRep) {
                newRepos.add(parseGAV(r, this::resolveSingle));
            }
        }
        return newRepos;
    }

    private DefaultExternalModuleDependency asDependency(ArtifactDescriptor artifact) {
        DefaultExternalModuleDependency dep = new DefaultExternalModuleDependency(artifact.getGroup(), artifact.getName(), artifact.getVersion());
        dep.setTransitive(false);
        dep.setArtifacts(Collections.singleton(new DefaultDependencyArtifact(dep.getName(), artifact.getType(), artifact.getExtension(), artifact.getClassifier(), null)));
        return dep;
    }

    public File resolveSingle(ArtifactDescriptor artifact) {
        String cname = "featureRepoDynamic_" + artifact.getGroup() + "_" + artifact.getName() + "_" + artifact.getClassifier() + "_" + artifact.getVersion();
        Configuration dynamicFeatureRepos = getProject().getConfigurations().findByName(cname);
        if (dynamicFeatureRepos == null) {
            dynamicFeatureRepos = getProject().getConfigurations().create(cname);
            DefaultExternalModuleDependency dep = asDependency(artifact);
            dynamicFeatureRepos.getDependencies().add(dep);
        }
        Set<File> resolved = dynamicFeatureRepos.resolve();
        if (resolved != null && resolved.size() >= 1) {
            ResolvedArtifact rartifact = dynamicFeatureRepos.getResolvedConfiguration().getResolvedArtifacts().iterator().next();
            artifact.setType(rartifact.getType());
            artifact.setExtension(rartifact.getExtension());
            return resolved.iterator().next();
        } else {
            throw new IllegalArgumentException("No result for " + artifact);
        }

    }

    public void materialize(Set<ArtifactDescriptor> artifacts,File target) throws Exception {
        Map<String, File> toBeGenerated = new HashMap<>();
        feed(toBeGenerated, artifacts);
        writeAll(toBeGenerated, target);
    }

    private void writeAll(Map<String, File> toBeGenerated, File target) throws IOException {
        for (String path : toBeGenerated.keySet()) {
            File out = new File(target, path);
            if (!out.exists()) {
                write(out, Okio.source(toBeGenerated.get(path)));
            }
        }
    }

    private void write(File out, Source data) throws IOException {
        project.getLogger().info("Inserted Artifact: " + out.getAbsolutePath());
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }

        try (BufferedSource source = buffer(data)) {
            try (BufferedSink sink = buffer(Okio.sink(out))) {
                sink.writeAll(source);
            }
        }
    }

    private void feed(Map<String, File> container, Set<ArtifactDescriptor> artifacts) {
        for (ArtifactDescriptor thing : artifacts) {
            File f = thing.resolve();
            // create path name:
            String path = thing.getGroup().replaceAll("\\.", "/")
                    + "/" + thing.getName()
                    + "/" + thing.getVersion()
                    + "/";
            if (thing.getClassifier() == null) {
                path += thing.getName() + "-" + thing.getVersion()
                        + "." + thing.getExtension();
            } else {
                path += thing.getName() + "-" + thing.getVersion()
                        + "-" + thing.getClassifier()
                        + "." + thing.getExtension();
            }
            container.put(path, f);
        }
    }

    public Project getProject()
    {
        return project;
    }
}
