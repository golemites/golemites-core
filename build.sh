#!/usr/bin/env bash
set -ex
mkdir -p build/native-image-config/META-INF/native-image

./gradlew build && $GRAALVM_HOME/bin/java \
  -agentlib:native-image-agent=config-output-dir=build/native-image-config/META-INF/native-image \
  -jar golemites-osgi-launcher/build/libs/golemites-osgi-launcher-0.1.0-SNAPSHOT.jar $*

  $GRAALVM_HOME/bin/native-image \
  --no-fallback \
  --no-server \
  --report-unsupported-elements-at-runtime \
  -cp build/native-image-config/:./golemites-osgi-launcher/build/libs/golemites-osgi-launcher-0.1.0-SNAPSHOT.jar \
  -H:Name=build/golem \
  --initialize-at-build-time= \
  -H:Class=org.golemites.launcher.Launcher \
  -H:+ReportExceptionStackTraces \
  -H:-AddAllCharsets \
  -H:-SpawnIsolates \
  -H:+JNI \
  -H:+UseServiceLoaderFeature \
  -H:+StackTrace \
  -H:+AllowIncompleteClasspath

build/./golem $*