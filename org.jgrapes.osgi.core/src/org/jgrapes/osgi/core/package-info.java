/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2022  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

/**
 * This package provides support for integrating JGrapes with OSGi 
 * services.
 * 
 * There are several ways in which JGrapes and OSGi can be integrated.
 * 
 *  * Provide libraries as OSGi bundle. This is done for all JGrapes 
 *    libraries.
 *    
 *  * Write an OSGi bundle with an activator that adds one or more
 *    components to a JGrapes component tree. This is possible but 
 *    not recommended as a general approach because 
 *    
 *     * the component tree has to be made accessible in the OSGi 
 *       service registry (not really a problem) and
 *       
 *     * the activator has to "know" where to insert the components
 *       in the tree, thus introducing a dependency of the bundle
 *       on the application and limiting the possibilities for reuse.
 *      
 *  * Use a variation of the 
 *    "[Whiteboard Pattern](https://web.archive.org/web/20220507070515/http://docs.osgi.org/whitepaper/whiteboard-pattern/)".
 *    
 *  This package provides the helper class 
 *  {@link org.jgrapes.osgi.core.ComponentCollector} that simplifies the
 *  implementation of the third approach.
 * 
 * @startuml package-hierarchy.svg
 * skinparam svgLinkTarget _parent
 * 
 * package org.jgrapes {
 * 
 *     package "org.jgrapes.*" {
 *         note "JGrapes comoponent libraries" as LibrariesNote
 *     }
 * 
 *     package org.jgrapes.osgi {
 *         package org.jgrapes.osgi.core [[./package-summary.html#package.description]] {
 *         }
 *         
 *         package org.jgrapes.osgi.webconlet.logviewer \
 *         [[../webconlet/logviewer/package-summary.html#package.description]] {
 *         }
 *         
 *         package org.jgrapes.osgi.webconlet.bundles \
 *         [[../webconlet/bundles/package-summary.html#package.description]] {
 *         }
 *         
 *         package org.jgrapes.osgi.webconlet.services \
 *         [[../webconlet/services/package-summary.html#package.description]] {
 *         }
 *         
 *     }
 * 
 * }
 * 
 * "org.jgrapes.*" <.. org.jgrapes.osgi.core
 * "org.jgrapes.*" <.. org.jgrapes.osgi.webconlet.logviewer
 * "org.jgrapes.*" <.. org.jgrapes.osgi.webconlet.bundles
 * "org.jgrapes.*" <.. org.jgrapes.osgi.webconlet.services
 * 
 * org.jgrapes.osgi.core -[hidden]right-> org.jgrapes.osgi.webconlet.logviewer
 * org.jgrapes.osgi.webconlet.logviewer -[hidden]up-> org.jgrapes.osgi.webconlet.bundles
 * org.jgrapes.osgi.webconlet.logviewer -[hidden]up-> org.jgrapes.osgi.webconlet.services
 * 
 * @enduml
 */
package org.jgrapes.osgi.core;