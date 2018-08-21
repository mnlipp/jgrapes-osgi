/*
 * Ad Hoc Polling Application
 * Copyright (C) 2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.osgi.demo.portal;

import java.net.URI;

import org.jgrapes.core.Channel;
import org.jgrapes.portal.bootstrap4.Bootstrap4Weblet;

/**
 *
 */
public class DemoPortalWeblet extends Bootstrap4Weblet {

    /**
     * Instantiates a new demo portal weblet.
     *
     * @param webletChannel the weblet channel
     * @param portalChannel the portal channel
     * @param portalPrefix the portal prefix
     */
    public DemoPortalWeblet(Channel webletChannel, Channel portalChannel,
            URI portalPrefix) {
        super(webletChannel, portalChannel, portalPrefix);
    }

}
