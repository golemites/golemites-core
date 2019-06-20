package org.tablerocket.febo.plugin.resolver;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static okio.Okio.buffer;
import static org.tablerocket.febo.plugin.resolver.ArtifactDescriptor.parseGAV;

public class FeatureRepositoryResolverTask {

    public static final String INPUT_CONFIG = "toni";
    private Project project;

    public Set<ArtifactDescriptor> loadArtifactsTransitively(Project project) throws Exception {
        this.project = project;

        Configuration featureRepo = project.getConfigurations().maybeCreate(INPUT_CONFIG);
        Set<ArtifactDescriptor> givenRepos = findTransitiveKaraFeatureRepositories(featureRepo);
        Set<FeaturedBundle> bundleRefs = new CompositeReadableFeatureRepository().collect(givenRepos);

        // convert them to artifacts (aka parsing):
        Set<ArtifactDescriptor> resolvedBundles = bundleRefs.stream()
                .map((s) -> parseGAV(s.url, this::resolveSingle))
                .collect(Collectors.toSet());
        resolvedBundles.addAll(givenRepos);

        return resolvedBundles; //materialize(resolvedBundles,target);
    }

    private Set<ArtifactDescriptor> findTransitiveKaraFeatureRepositories(Configuration featureRepo) throws IOException {
        Set<ArtifactDescriptor> givenRepos = extractRepositoryDependencies(featureRepo);
        Set<Source> builtIn = resolveAll(featureRepo);
        Set<ArtifactDescriptor> newRepos = findEmbeddedRepos(builtIn.toArray(new Source[builtIn.size()]));

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

    private Set<ArtifactDescriptor> extractRepositoryDependencies(Configuration featureRepo) {
        Set<ArtifactDescriptor> givenRepos = new HashSet<>();
        for (Dependency dep : featureRepo.getDependencies()) {
            if (dep instanceof DefaultExternalModuleDependency) {
                DefaultExternalModuleDependency extDep = (DefaultExternalModuleDependency) dep;
                for (DependencyArtifact artifact : extDep.getArtifacts()) {
                    ArtifactDescriptor desc = new ArtifactDescriptor(this::resolveSingle);
                    desc.setGroup(dep.getGroup());
                    desc.setName(dep.getName());
                    desc.setVersion(dep.getVersion());
                    desc.setType(artifact.getType());
                    desc.setClassifier(artifact.getClassifier());
                    desc.setExtension(artifact.getExtension());
                    givenRepos.add(desc);
                    if (desc.getType() == null) {
                        throw new IllegalArgumentException("Artifact " + desc + " has nulltype: " + extDep.getArtifacts().size());
                    }
                    if (desc.getName() == null) {
                        throw new IllegalArgumentException("Artifact " + desc + " has null name: " + dep.getName());
                    }
                }
            }
        }
        if (givenRepos.size() == 0) {
          //  throw new GradleException("There is no valid karaf repository set up for configuration " + INPUT_CONFIG);
        }
        return givenRepos;
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
