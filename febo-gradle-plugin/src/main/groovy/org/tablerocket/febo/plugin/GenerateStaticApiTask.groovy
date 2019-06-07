package org.tablerocket.febo.plugin

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
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
import org.tablerocket.febo.api.Dependency
import org.tablerocket.febo.plugin.resolver.ArtifactDescriptor
import org.tablerocket.febo.plugin.resolver.FeatureRepositoryResolverTask
import org.tablerocket.febo.repository.RepositoryStore

import javax.lang.model.element.Modifier

@CacheableTask
class GenerateStaticApiTask extends DefaultTask {

    def File outputLocation

    def String packageName

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
        Properties p = new Properties()

        def project = getProject()
        def now = '' //new Date() disabled as it would nuke the build-cache + up to date checks.
        outputLocation.mkdirs()


        Set<ArtifactDescriptor> artifacts = new FeatureRepositoryResolverTask().loadArtifactsTransitively(project)


        def loc = project.configurations.getByName("baseline").resolvedConfiguration.firstLevelModuleDependencies
        for (ResolvedDependency rs : loc) {

            for (ResolvedArtifact art : rs.moduleArtifacts) {
                println "First level: " + art.file.name
                ArtifactDescriptor desc = new ArtifactDescriptor(art.file)
                desc.name = art.name
                desc.group = art.moduleVersion.id.group
                desc.extension = art.extension
                desc.type = art.type
                desc.version = art.moduleVersion.id.version
                artifacts.add(desc)
                for (PropertyValue pv : art.metaPropertyValues)
                {
                    getLogger().warn("--> " + pv.name + "=" + pv.value)
                }
            }
        }


        createCompileDependenciesApiClazz(project, session, p,artifacts)

        // Store the very blobstore index for now in a plain file here:
        File db = new File(generatedResourcesDir,"febo-blobs.properties");
        db.getParentFile().mkdirs();
        p.store(new FileWriter(db),"")
    }

    private createCompileDependenciesApiClazz(Project project, TreeSession session, Properties p,Set<ArtifactDescriptor> karafRepoArtifacts) {

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
        for (ArtifactDescriptor art : karafRepoArtifacts) {
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
                    .addJavadoc('version=$N',art.version)
                    .returns(Dependency.class)
                    .addStatement('return this.backend.resolve($S)',tree.value().hash())
                    .build()
            /**
            def fieldSpec = FieldSpec.builder(String.class, name).addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .initializer('$S', tree.fingerprint())
                    .build()
         **/
            p.put(tree.value().hash(), art.resolve().absolutePath)
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
                    String bsn = tb.getHeader("Bundle-SymbolicName");
                    String version = tb.getHeader("Bundle-Version");

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
