/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017 Michael N. Lipp
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

package org.jgrapes.osgi.http;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.osgi.factory.http.HttpRequestHandlerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * 
 */
public class HttpRequestHandlerCollector extends Component 
	implements ServiceTrackerCustomizer
		<HttpRequestHandlerFactory<?>, HttpRequestHandlerFactory<?>> {

	private BundleContext context;
	private ServiceTracker<HttpRequestHandlerFactory<?>, HttpRequestHandlerFactory<?>> 
		serviceTracker;
	
	/**
	 * @param componentChannel
	 */
	public HttpRequestHandlerCollector(Channel componentChannel, BundleContext context) {
		super(componentChannel);
		this.context = context;
		@SuppressWarnings("unchecked")
		Class<HttpRequestHandlerFactory<?>> cls 
			= (Class<HttpRequestHandlerFactory<?>>)(Class<?>)HttpRequestHandlerFactory.class;
		serviceTracker = new ServiceTracker<>(
				context, cls, this);
		serviceTracker.open();
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public HttpRequestHandlerFactory<?> addingService(
	        ServiceReference<HttpRequestHandlerFactory<?>> reference) {
		HttpRequestHandlerFactory<?> handlerFactory = context.getService(reference);
		if (StreamSupport.stream(spliterator(), false)
				.filter(c -> c.getClass()
						.equals(handlerFactory.componentType()))
				.count() == 0) {
			Map<Object,Object> props = new HashMap<>();
			props.put(BundleContext.class, context);
			attach(handlerFactory.create(channel(), props));
		}
		return handlerFactory;
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void modifiedService(ServiceReference<HttpRequestHandlerFactory<?>> reference,
			HttpRequestHandlerFactory<?> service) {
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(ServiceReference<HttpRequestHandlerFactory<?>> reference,
			HttpRequestHandlerFactory<?> service) {
		for (ComponentType child: this) {
			if (child.getClass().equals(service.componentType())) {
				Components.manager(child).detach();
			}
		}
	}

}
