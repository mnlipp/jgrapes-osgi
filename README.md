JGrapes OSGi
===========

[![Build Status](https://travis-ci.org/mnlipp/jgrapes-osgi.svg?branch=master)](https://travis-ci.org/mnlipp/jgrapes-osgi)

See the [project's home page](http://mnlipp.github.io/jgrapes/).

This repository comprises JGrapes components that are built on top of the 
[library components](https://github.com/mnlipp/jgrapes) 
and depend on the OSGi framework. They have been put in a seperate repository 
because they require a different top-level build approach than the libraries
that can be used as OSGi bundles but do not depend on the OSGi framework.

Building
--------

The project is organized as `bnd` workspace project. The libraries can 
be built with `gradle build`. For working with the project in Eclipse 
run `gradle eclipse` before importing the project. Make sure that you 
have installed [Bndtools](http://bndtools.org/).

<!-- Piwik Image Tracker-->
<img src="https://piwik.mnl.de/piwik.php?idsite=10&rec=1&url=https%3A%2F%2Fgithub.com%2Fmnlipp%2Fjgrapes-osgi" style="border:0" alt="" />
<!-- End Piwik -->
