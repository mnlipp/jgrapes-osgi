An event driven component framework.

JGrapes OSGi
===========

JGrapes OSGi comprises the OSGi dependend components of the JGrapes component framework.

<object type="image/svg+xml" data="package-hierarchy.svg">Package hierarchy</object>

`org.jgrapes.osgi.factory`
: This package combines the service APIs for the various component types. See the
    <a href="org/jgrapes/osgi/factory/package-summary.html#package.description">package description</a>
    for details.

@startuml package-hierarchy.svg
skinparam svgLinkTarget _parent

package org.jgrapes {

    package "org.jgrapes.*" {
    	note "JGrapes comoponent libraries" as LibrariesNote
    }

	package org.jgrapes.osgi {
    	package org.jgrapes.osgi.factory [[org/jgrapes/osgi/factory/package-summary.html#package.description]] {
    	}
    	
    	package org.jgrapes.osgi.httpserver [[org/jgrapes/osgi/httpserver/package-summary.html#package.description]] {
    	}
    	
    	package org.jgrapes.osgi.portal [[org/jgrapes/osgi/portal/package-summary.html#package.description]] {
    	}
	}

}

"org.jgrapes.*" <.. org.jgrapes.osgi

org.jgrapes.osgi.factory <.. org.jgrapes.osgi.httpserver
org.jgrapes.osgi.factory <.. org.jgrapes.osgi.portal


@enduml