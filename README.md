JGrapes OSGi
===========

[![Build Status](https://travis-ci.org/mnlipp/jgrapes-osgi.svg?branch=master)](https://travis-ci.org/mnlipp/jgrapes-osgi)


| Bundle          | Bintray |
| --------------- | ------- |
| OSGi Core       | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.core/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.core/_latestVersion)
| OSGi Demo Portal | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.demo.portal/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.demo.portal/_latestVersion)
| Bundles Portlet | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.portlets.bundles/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.portlets.bundles/_latestVersion)
| Services Portlet | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.portlets.services/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.portlets.services/_latestVersion)
| Logviewer Portlet | [ ![Download](https://api.bintray.com/packages/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.portlets.logviewer/images/download.svg) ](https://bintray.com/mnlipp/jgrapes/org.jgrapes%3Aorg.jgrapes.osgi.portlets.logviewer/_latestVersion)

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
