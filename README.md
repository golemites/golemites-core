[![OSGi powered](https://img.shields.io/badge/OSGi-powered-blue.svg)](http://www.osgi.org)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/nebula-publishing-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

# Febo

Febo is a concept study for alternative ways to develop modular software.

Febo stands for "Felix Boot" but the project has evolved since that, so it is not really meaning that anymore.

It is a set of independent concepts that may or may not make sense altogether at the moment.

## Concept Ideas

![Febo Concepts](febo1.jpg)

Remember that all of those concepts are independent from each other and may or may not end up in an actual release.

### Flat Class Space

This application has a single gradle module that contains all resources and classes. 

There is no need to break up parts initially. The only physical separation that is suggested is the use of java packages.
This is natural to any Java Developer.

If an API/Interface should be shared beyond this application, it can be put into its own maven/gradle submodule.

### Run without tool or IDE plugin

Febo is a stark contrast to industry trend that suggests putting the heavy lifting into the build phase. (e.g. Quarkus)

Instead, febo starts in any environment (Main Class, plain Junit Test, embedded somewhere) and assembles the application (bundles) upon start.

This allows "perfect fit for context" applications.

It also allows adaptive applications because Febo controls every bundle lifecycle - yet it even MAKES the bundles.

### Typed Dependencies

Dependencies used to be strings either in a text file (karaf) or application code (pax exam).

With the help of a (gradle-) plugin, dependencies can be expressed as java types and used within febo startup code leveraging code completion etc.

The use of automatic resolution using OSGi Resolver is also possible (yet not implemented).

Here the main idea is to say: if it compiles, your dependencies are available and "ok to be used" - because they are part of a curated set.

The current concept also separates dependency identity (currently the sha-256) from its physical location.

A packaged febo application (not done yet) might embedd its dependencies or might delegate to downloaders like pax url.

### Optional isolation (adhoc bundling)

Febo can package up the business logic of an application from a given classpath (usually its own one) in one or more distinct bundles.

You can imagine that all code below the "Main" Class (package-wise) may end up in a single fat bundle with no exports by default.

You can also imagine that you can have marker classes (here it is the CalculatorBundle class) that mark an isolation point saying: "all code below this marker class should become its own bundle".

Of cause, adhoc bundling does not mean that febo might cache previously created bundles across restarts.

### Functional invocation

The current default behaviour of febo is that after boot it will try to find a special service (FeboEntrypoint) and invoke that with parameters given.

By default it will kill the framework after executing that entrypoint method.

This makes the whole thing a "functional" application.

If a server-like behaviour is desired, we do have a keepRunning(boolean) api that makes lets the framework alive after executing the entrypoint.




