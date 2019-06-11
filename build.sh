#!/usr/bin/env bash

##############################################################################
##
##  Build Script
##
##############################################################################

# Make sure you've got recent version of 
# - Rebaze Integrity Library

# Enforce a full rebuild

./gradlew publishToMavenLocal && ./gradlew :febo-baseline:clean --P baseline=true && ./gradlew :febo-baseline:jar --rerun-tasks :febo-baseline:publishToMavenLocal --P baseline=true