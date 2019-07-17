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

./gradlew publishToMavenLocal && ./gradlew :$ARTIFACT:publishToMavenLocal --P baseline=true -i --stacktrace