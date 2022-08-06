/*
 * Ad Hoc Polling Application
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.osgi.demo.console;

import java.net.URI;
import org.jgrapes.core.Channel;
import org.jgrapes.webconsole.vuejs.VueJsConsoleWeblet;

/**
 *
 */
public class DemoConsoleWeblet extends VueJsConsoleWeblet {

    /**
     * Instantiates a new demo console weblet.
     *
     * @param webletChannel the weblet channel
     * @param consoleChannel the web console channel
     * @param consolePrefix the web console prefix
     */
    public DemoConsoleWeblet(Channel webletChannel, Channel consoleChannel,
            URI consolePrefix) {
        super(webletChannel, consoleChannel, consolePrefix);
    }

}
