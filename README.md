JGrapes OSGi
============

[![Build Status](https://github.com/mnlipp/jgrapes-osgi/workflows/Java%20CI/badge.svg)](https://github.com/mnlipp/jgrapes-osgi/actions)


| Bundle          | Download |
| --------------- | -------- |
| OSGi Core       | [![Maven Central](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.osgi.core.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.osgi.core%22)
| OSGi Demo Portal | [![Maven Central](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.osgi.demo.portal.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.osgi.demo.portal%22)
| Bundles Portlet | [![Maven Central](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.osgi.portlets.bundles.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.osgi.portlets.bundles%22)
| Services Portlet | [![Maven Central](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.osgi.portlets.services.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.osgi.portlets.services%22)
| Logviewer Portlet | [![Maven Central](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.osgi.portlets.logviewer.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.osgi.portlets.logviewer%22)

See the [project's home page](https://mnlipp.github.io/jgrapes/).

This repository comprises JGrapes components that are built on top of the 
[library components](https://github.com/mnlipp/jgrapes) 
and depend on the OSGi framework. They have been put in a seperate repository 
because they require a different top-level build approach than the libraries
that can be used as OSGi bundles but do not depend on the OSGi framework.

Building
--------

The project is organized as a `bnd` 
[workspace build](https://github.com/bndtools/bnd/blob/master/biz.aQute.bnd.gradle/README.md#gradle-plugins-for-bnd-workspace-builds).
The bundles can 
be built with `gradle build`. For working with the project in Eclipse 
run `gradle eclipse` before importing the project. Make sure that you 
have installed [Bndtools](http://bndtools.org/).
