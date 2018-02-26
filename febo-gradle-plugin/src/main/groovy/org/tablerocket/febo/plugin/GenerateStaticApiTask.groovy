package org.tablerocket.febo.plugin

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.rebaze.integrity.tree.api.Tree
import org.rebaze.integrity.tree.api.TreeBuilder
import org.rebaze.integrity.tree.api.TreeSession
import org.rebaze.integrity.tree.util.DefaultTreeSessionFactory

import javax.lang.model.element.Modifier

@CacheableTask
class GenerateStaticApiTask extends DefaultTask {

    def File outputLocation

    def String packageName

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
        def artifacts = project.configurations.getByName("compile").resolvedConfiguration.resolvedArtifacts


        // POET HERE
        def compileDepBuilder = TypeSpec.classBuilder("CompileDependencies")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        for (ResolvedArtifact art : artifacts) {
            Tree tree = session.createStreamTreeBuilder().add(art.file).seal()

            def fieldSpec = FieldSpec.builder(String.class,art.name.replaceAll("-","_").replaceAll("\\.","_").toUpperCase()
            ).addModifiers(Modifier.FINAL,Modifier.STATIC)
            .initializer('$S',tree.fingerprint())
            .build()
            p.put(tree.fingerprint(),art.file.absolutePath)

            compileDepBuilder.addField(fieldSpec)
        }
        p.store(new FileWriter(new File(outputLocation,packageName + ".properties")),"")

        JavaFile javaFile = JavaFile.builder(packageName, compileDepBuilder.build()).build()
        javaFile.writeTo(outputLocation)

    }
}
