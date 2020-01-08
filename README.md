[![OSGi powered](https://img.shields.io/badge/OSGi-powered-blue.svg)](http://www.osgi.org)
[![Kubernetes aware](https://img.shields.io/badge/Kubernetes-aware-blue.svg)](http://www.kubernetes.org)
[![Build Status](https://github.com/golemites/golemites/workflows/cibuild/badge.svg)](https://github.com/golemites/golemites/workflows/cibuild/)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/nebula-publishing-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

# Project Golemites

This project is work in progress. First public demos are scheduled for Q1 2020.

Contact Email dev@rebaze.com or twitter @tonit & @rebazeio.

## Concepts

### Platform + Application

In Golemites a deployment is made of two things that we separate in a very hard way: 
* Tailored Platform - the fundamental part that is tailored to the needs of the application. Made from underlying runtime and a so called "Gestalt".
* Application - the components that contain mostly business logic, domain types, functionality.

In Golemites, both "parts" are in your control, but heavily separated:
* The "Gestalt" that makes the platform specific is a defined step with its own lifecycle, APIs. To begin with golemites project provides *Gestalts* to get started. 
* The Application is your typical application that is restricted to the gestalt selected. BUT with the huge benefit that things just work, testing and deployment is a breeze, security patches are "streamed" to you.

### Gestalt

This is probably the most differentiating factor.
Traditionally developers are responsible for selecting API abstractions, finding implementations, updating to newer versions, making sure there
not too much duplication in used "solutions", looking for synergy effects etc.
Now, a gestalt in golemites is a carefully built artifact that encompasses exactly the API surface that should be exposed to the application layer.

### Autobundle

Instead of relying on special plugins and IDEs to create the actual deployment form - e.g. an OSGi Bundle - we assume idiomatic java libraries.
The actual transformation into deployable bundles is the last step implemented by a Golemites Feature called Autobundle.

### Cloud Native DevOps

Golemites comes with Gradle Plugins that automates build & deployment of transport-sensitive deployment units (OCI-Images) as well as Kubernetes Resource Management.

## Project Principles

* Opinionated - means that we strive for developer experience instead for generic solutions.
* Curated - mean that we a provide "batteries included" solution that just works.  
* Idiomatic - means that we are developer focused, letting them write code instead of tackling undifferentiated heavy-lifting.

## Design Decisions

* Kubernetes on macro level - K8s is the super-container that allows high automation & extreme scalability.
* OSGi on micro level - OSGi is the most advanced micro-level container within a single JVM.

Golemites strives to bring micro- and macro level deployment units to a productive synergy.

## Batteries Included

* Curated APIs with matching providers
* Continuous deployment plugins for Gradle
* Testing harness based on the Junit 5 Platform
* Idiomatic liveness & readiness probes for kubernetes
* Yaml-free experience
