#!/usr/bin/env bash

##############################################################################
##
##  Build Script
##
##############################################################################

# Make sure you've got recent version of 
# - Rebaze Integrity Library

# Enforce a full rebuild

export ARTIFACT=febo-example-baseline

./gradlew publishToMavenLocal && ./gradlew :$ARTIFACT:clean :$ARTIFACT:clean :$ARTIFACT:jar --stacktrace --P baseline=true  --rerun-tasks && ./gradlew :$ARTIFACT:publishToMavenLocal --rerun-tasks --P baseline=true