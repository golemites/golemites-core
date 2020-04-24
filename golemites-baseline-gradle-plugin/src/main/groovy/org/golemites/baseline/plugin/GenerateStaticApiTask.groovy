package org.golemites.baseline.plugin


import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.golemites.baseline.plugin.resolver.ArtifactDescriptor
import org.golemites.baseline.plugin.synth.FlatCopyMirror
import org.golemites.baseline.plugin.synth.Mirror
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.ops4j.pax.tinybundles.core.TinyBundle
import org.ops4j.pax.tinybundles.core.TinyBundles
import org.rebaze.integrity.tree.api.Tree
import org.rebaze.integrity.tree.api.TreeSession
import org.rebaze.integrity.tree.util.DefaultTreeSessionFactory
import org.golemites.api.Dependency
import org.golemites.api.Metadata
import org.golemites.api.RepositoryStore
import org.golemites.api.TargetPlatformSpec

import javax.lang.model.element.Modifier

@CacheableTask
class GenerateStaticApiTask extends DefaultTask {

    def File outputLocation

    def String packageName

    @OutputDirectory
    def File generatedResourcesDir

    @OutputDirectory
    def File getOutputLocation() {
        return outputLocation
    }

    @Input
    def String getPackageName() {
        return packageName
    }

    def File setOutputLocation(File f) {
        outputLocation = f
    }

    def String setPackageName(String pack) {
        packageName = pack
    }

    @TaskAction
    void makeVersionClass() throws IOException {
        TreeSession session = new DefaultTreeSessionFactory().create()

        def project = getProject()
        outputLocation.mkdirs()

        // repository api

        List<ArtifactDescriptor> repositoryArtifacts = new ArrayList<>()

        def repositoryConfigs = project.configurations.getByName("repository").resolvedConfiguration.firstLevelModuleDependencies
        for (ResolvedDependency rs : repositoryConfigs) {
            for (ResolvedArtifact art : rs.moduleArtifacts) {
                getLogger().info "+ Repository : " + art.file.name
                ArtifactDescriptor desc = new ArtifactDescriptor(art.file)
                desc.name = art.name
                desc.group = art.moduleVersion.id.group
                desc.extension = art.extension
                desc.type = art.type
                desc.version = art.moduleVersion.id.version
                repositoryArtifacts.add(desc)
            }
        }
        createCompileDependenciesApiClazz(project, session, repositoryArtifacts)
        // Store the very blobstore index for now in a plain file here:
        File db = new File(generatedResourcesDir,"febo-blobs.properties");
        db.getParentFile().mkdirs();
        // write new format:
        ObjectMapper mapper = new ObjectMapper()
        TargetPlatformSpec platform = new TargetPlatformSpec()

        List<Dependency> deps = new ArrayList<Dependency>()
        for (ArtifactDescriptor ad : repositoryArtifacts) {
            Dependency rd = Dependency.dependency(
                    session.createStreamTreeBuilder().add(ad.resolve()).seal().value().hash(),
                    ad.resolve().toURI(),
                    Metadata.metadata(
                        ad.group,
                        ad.name,
                        ad.version,
                        ad.classifier,
                        ad.type
                    )
            )
            deps.add(rd)
        }
        platform.setDependencies(deps.toArray(new Dependency[deps.size()]))
        mapper.writeValue(new File(generatedResourcesDir,BLOB_FILENAME),platform)


        // baseline api
        Set<ArtifactDescriptor> baselineArtifacts = new HashSet<>()
        def baselineConfigs = project.configurations.getByName("baseline").resolvedConfiguration.firstLevelModuleDependencies
        getLogger().warn "+++++++  Aquire baseline : " + baselineConfigs.size()

        for (ResolvedDependency rs : baselineConfigs) {

            for (ResolvedArtifact art : rs.moduleArtifacts) {
                getLogger().info "+ Baseline : " + art.file.name
                ArtifactDescriptor desc = new ArtifactDescriptor(art.file)
                desc.name = art.name
                desc.group = art.moduleVersion.id.group
                desc.extension = art.extension
                desc.type = art.type
                desc.version = art.moduleVersion.id.version
                baselineArtifacts.add(desc)
            }
        }
        mirrorApis(project, session, baselineArtifacts)

    }

    private mirrorApis(Project project, TreeSession session,Set<ArtifactDescriptor> artifacts) {
        File out = new File(project.buildDir, 'classes/java/main')
        logger.info "Will write to " + out.absolutePath
        Mirror mirror = new FlatCopyMirror(out)

        for (ArtifactDescriptor art : artifacts) {
            // discover all classes
            File f = art.resolve()
            mirror.mirror(f)
        }
    }

    private createCompileDependenciesApiClazz(Project project, TreeSession session, List<ArtifactDescriptor> artifacts) {

        def compileDepBuilder = TypeSpec.classBuilder("FeboRepository")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                //.superclass(Repository.class)
                .addField(FieldSpec.builder(RepositoryStore.class,"backend").addModifiers(Modifier.FINAL,Modifier.PRIVATE)
                .build())
                .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(RepositoryStore.class, "backend")
                .addStatement('this.$N = $N', "backend", "backend")
                .build())

        Map<String,ArtifactDescriptor> index = new HashMap<>();
        for (ArtifactDescriptor art : artifacts) {
            //getLogger().warn(" + " + art.name)
            if ((art.resolve() == null || !art.resolve().exists())) {
                getLogger().warn(" - " + art.name + " bad because not existing: " + art.resolve())
                continue
            }
            String name = getNameFor(art)
            String version = typesafer(getVersionFor(art))
            ArtifactDescriptor given = index.get(name)
            if (given != null) {
                // clash. Fix it.
                String otherVersion = typesafer(getVersionFor(given))
                index.put(name + "_" + otherVersion, given)
                index.put(name + "_" + version,art)
            }else {
                index.put(name,art)
            }
        }

        for (String name : index.keySet()) {
            ArtifactDescriptor art = index.get(name)
            Tree tree = session.createStreamTreeBuilder().add(art.resolve()).seal()
            def methodSpec = MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc('group=$N name=$N version=$N',art.group,art.name,art.version)
                    .returns(Dependency.class)
                    .addStatement('return this.backend.resolve($S)',tree.value().hash())
                    .build()
           // compileDepBuilder.addField(fieldSpec)
            compileDepBuilder.addMethod(methodSpec)
        }

        JavaFile javaFile = JavaFile.builder(packageName, compileDepBuilder.build()).build()
        javaFile.writeTo(outputLocation)
    }

    private static String getNameFor(ArtifactDescriptor art) {
        File file = art.resolve()
        String name = art.name

        new FileInputStream(file).withCloseable {
            fis ->
                TinyBundle tb = TinyBundles.bundle().read(fis)
                if (fis != null) {
                    String bsn = tb.getHeader("Bundle-SymbolicName")
                    String version = tb.getHeader("Bundle-Version")

                    if (bsn != null) {
                        // fix any extra props:
                        int cap = bsn.indexOf(";")
                        if (cap >= 0) {
                            bsn = bsn.substring(0, cap)
                        }

                        if (version != null) {
                            return typesafer(bsn)
                        } else {
                            return typesafer(bsn)
                        }
                    }

                }
                return typesafer(name)
        }
    }

    private static String getVersionFor(ArtifactDescriptor art) {
        File file = art.resolve()

        new FileInputStream(file).withCloseable {
            fis ->
                TinyBundle tb = TinyBundles.bundle().read(fis)
                if (fis != null) {
                    String version = tb.getHeader("Bundle-Version");
                    if (version != null) {
                        return version
                    }
                }
                if (art.version != null) {
                    return art.version
                }else {
                    throw new IllegalArgumentException("Art " + art.name + " has null version.. " + art.resolve().getAbsolutePath())
                }
        }
    }

    private static String getNameFor(File file, String name) {
        new FileInputStream(file).withCloseable {
            fis ->
                TinyBundle tb = TinyBundles.bundle().read(fis)
                if (fis != null) {
                    String bsn = tb.getHeader("Bundle-SymbolicName");
                    String version = tb.getHeader("Bundle-Version");

                    if (bsn != null) {
                        // fix any extra props:
                        int cap = bsn.indexOf(";")
                        if (cap >= 0) {
                            bsn = bsn.substring(0, cap)
                        }
                        if (version != null) {
                            return typesafer(bsn + "_" + version)
                        }else {
                            return typesafer(bsn)
                        }
                    }

                }
                return typesafer(name)

        }

    }

    private static String typesafer(String s) {
        s.replaceAll("-", "_").replaceAll("\\.", "_")
    }

}
