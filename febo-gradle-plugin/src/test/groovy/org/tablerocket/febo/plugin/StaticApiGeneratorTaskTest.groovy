package org.tablerocket.febo.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class StaticApiGeneratorTaskTest extends Specification {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    File settingsFile

    def setup() {
        def f = testProjectDir.root

        buildFile = new File(f, 'build.gradle')
        settingsFile = new File(f, 'settings.gradle')
    }

    @spock.lang.Ignore
    def "generate"() {
        given:

        settingsFile << """
        rootProject.name = 'foo-bar'
        """

        buildFile << """
        
        plugins {
            id 'org.tablerocket.febo.plugin'
        }
        
        repositories {
            mavenCentral()
        }
        
        configurations {
            featureRepo
         }
        
        dependencies {
        }
        
        febo {
            packageName 'com.foo.bar'
        }
        
    """

        when:
        def result = GradleRunner.create()
                .withProjectDir(buildFile.parentFile)
                .withArguments('generateStaticApi')
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .build()

        then:
        result.task(":generateStaticApi").outcome == SUCCESS
        new File(buildFile.parentFile, "build/generated/java/com/foo/bar/FeboRepository.java").exists()
    }
}
