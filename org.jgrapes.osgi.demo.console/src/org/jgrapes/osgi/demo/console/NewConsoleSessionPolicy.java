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

package org.jgrapes.osgi.demo.console;

import java.util.Optional;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.webconsole.base.events.ConsoleConfigured;
import org.jgrapes.webconsole.base.events.RenderConlet;

/**
 * 
 */
public class NewConsoleSessionPolicy extends Component {

    private final String renderedFlagName = getClass().getName() + ".rendered";

    /**
     * Creates a new component with its channel set to itself.
     */
    public NewConsoleSessionPolicy() {
        // Everything is done by super.
    }

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel
     */
    public NewConsoleSessionPolicy(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Handles a conlet render request.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onRenderConlet(RenderConlet event, IOSubchannel channel) {
        channel.associated(Session.class)
            .ifPresent(session -> session.put(renderedFlagName, true));
    }

    /**
     * On console configured.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     */
    @Handler
    @SuppressWarnings("PMD.CollapsibleIfStatements")
    public void onConsoleConfigured(ConsoleConfigured event,
            IOSubchannel channel) throws InterruptedException {
        Optional<Session> optSession = channel.associated(Session.class);
        if (optSession.isPresent()) {
            if ((Boolean) optSession.get().getOrDefault(
                renderedFlagName, false)) {
                return;
            }
        }
    }

}
