/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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