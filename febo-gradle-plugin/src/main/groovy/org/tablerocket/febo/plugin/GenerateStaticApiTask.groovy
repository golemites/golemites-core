package org.tablerocket.febo.plugin

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.ops4j.pax.tinybundles.core.TinyBundle
import org.ops4j.pax.tinybundles.core.TinyBundles
import org.rebaze.integrity.tree.api.Tree
import org.rebaze.integrity.tree.api.TreeBuilder
import org.rebaze.integrity.tree.api.TreeSession
import org.rebaze.integrity.tree.util.DefaultTreeSessionFactory
import org.tablerocket.febo.plugin.resolver.ArtifactDescriptor
import org.tablerocket.febo.plugin.resolver.FeatureRepositoryResolverTask

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

        // Static compile dependencies as java:
        createCompileDependenciesApiClazz(project, session, p)

        Set<ArtifactDescriptor> karafRepoArtifacts = new FeatureRepositoryResolverTask().loadArtifactsTransitively(project)
        createCompileDependenciesApiClazz(project, session, p,karafRepoArtifacts)

        // Store the very blobstore index for now in a plain file here:
        // TODO: Fix this location as this file will not be copied.
        File db = new File(generatedResourcesDir,packageName + ".properties");
        db.getParentFile().mkdirs();
        p.store(new FileWriter(db),"")
    }

    private createCompileDependenciesApiClazz(Project project, TreeSession session, Properties p) {
        def artifacts = project.configurations.getByName("compile").resolvedConfiguration.resolvedArtifacts

        def compileDepBuilder = TypeSpec.classBuilder("CompileDependencies")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        for (ResolvedArtifact art : artifacts) {
            if (!art.file.exists()) continue;
            Tree tree = session.createStreamTreeBuilder().add(art.file).seal()
            String name = getNameFor(art.file,art.name).toUpperCase()

            def fieldSpec = FieldSpec.builder(String.class, name)
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC,Modifier.STATIC)
                    .initializer('$S', tree.fingerprint())
                    .build()
            p.put(tree.fingerprint(), art.file.absolutePath)
            compileDepBuilder.addField(fieldSpec)
        }


        JavaFile javaFile = JavaFile.builder(packageName, compileDepBuilder.build()).build()
        javaFile.writeTo(outputLocation)
    }

    private createCompileDependenciesApiClazz(Project project, TreeSession session, Properties p,Set<ArtifactDescriptor> karafRepoArtifacts) {
        def compileDepBuilder = TypeSpec.classBuilder("RepositoryDependencies")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)


        for (ArtifactDescriptor art : karafRepoArtifacts) {
            Tree tree = session.createStreamTreeBuilder().add(art.resolve()).seal()

            String name = getNameFor(art.resolve(),art.name).toUpperCase()

            def fieldSpec = FieldSpec.builder(String.class, name
            ).addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                    .initializer('$S', tree.fingerprint())
                    .build()
            p.put(tree.fingerprint(), art.file.absolutePath)
            compileDepBuilder.addField(fieldSpec)
        }

        JavaFile javaFile = JavaFile.builder(packageName, compileDepBuilder.build()).build()
        javaFile.writeTo(outputLocation)
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
