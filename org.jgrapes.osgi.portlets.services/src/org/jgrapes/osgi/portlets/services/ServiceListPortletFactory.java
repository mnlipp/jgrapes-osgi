/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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

package org.jgrapes.osgi.portlets.services;

import java.util.Map;
import java.util.Optional;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.portal.PortletComponentFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

/**
 * The factory service for {@link ServiceListPortlet}s.
 */
@org.osgi.service.component.annotations.Component(
    scope = ServiceScope.SINGLETON)
public class ServiceListPortletFactory implements PortletComponentFactory {

    private ServiceComponentRuntime scr;

    @Reference
    public void setScr(ServiceComponentRuntime scr) {
        this.scr = scr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.ComponentFactory#componentType()
     */
    @Override
    public Class<? extends ComponentType> componentType() {
        return ServiceListPortlet.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.ComponentFactory#create(org.jgrapes.core.Channel,
     * java.util.Map)
     */
    @Override
    public Optional<ComponentType> create(Channel componentChannel,
            Map<Object, Object> properties) {
        return Optional.of(new ServiceListPortlet(componentChannel,
            (BundleContext) properties.get(BundleContext.class), scr));
    }

}
