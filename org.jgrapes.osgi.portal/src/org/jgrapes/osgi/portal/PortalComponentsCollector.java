/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

package org.jgrapes.osgi.portal;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.osgi.factory.portal.provider.PageResourceProviderFactory;
import org.jgrapes.osgi.factory.portlet.PortletFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * 
 */
public class PortalComponentsCollector extends Component {

	private ServiceTracker<PageResourceProviderFactory<?>, 
		PageResourceProviderFactory<?>> providerTracker;
	private ServiceTracker<PortletFactory<?>, PortletFactory<?>> portletTracker;
	
	/**
	 * @param componentChannel
	 */
	public PortalComponentsCollector(Channel componentChannel, BundleContext context) {
		super(componentChannel);
		
		// Tracker for page resource providers
		@SuppressWarnings("unchecked")
		Class<PageResourceProviderFactory<?>> cls 
			= (Class<PageResourceProviderFactory<?>>)(Class<?>)PageResourceProviderFactory.class;
		providerTracker = new ServiceTracker<>(context, cls,
				new ServiceTrackerCustomizer<PageResourceProviderFactory<?>, PageResourceProviderFactory<?>> () {
			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
			 */
			@Override
			public PageResourceProviderFactory<?> addingService(
			        ServiceReference<PageResourceProviderFactory<?>> reference) {
				PageResourceProviderFactory<?> providerFactory = context.getService(reference);
				if (StreamSupport.stream(spliterator(), false)
						.filter(c -> c.getClass()
								.equals(providerFactory.componentType()))
						.count() == 0) {
					Map<Object,Object> props = new HashMap<>();
					props.put(BundleContext.class, context);
					attach(providerFactory.create(channel(), props));
				}
				return providerFactory;
			}

			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
			 */
			@Override
			public void modifiedService(ServiceReference<PageResourceProviderFactory<?>> reference,
					PageResourceProviderFactory<?> service) {
			}

			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
			 */
			@Override
			public void removedService(ServiceReference<PageResourceProviderFactory<?>> reference,
					PageResourceProviderFactory<?> service) {
				for (ComponentType child: PortalComponentsCollector.this) {
					if (child.getClass().equals(service.componentType())) {
						Components.manager(child).detach();
					}
				}
			}
			
		});
		providerTracker.open();
		
		// Tracker for portlets
		@SuppressWarnings("unchecked")
		Class<PortletFactory<?>> portletFactoryCls 
			= (Class<PortletFactory<?>>)(Class<?>)PortletFactory.class;
		portletTracker = new ServiceTracker<>(context, portletFactoryCls, 
				new ServiceTrackerCustomizer<PortletFactory<?>, PortletFactory<?>> () {
			
			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
			 */
			@Override
			public PortletFactory<?> addingService(
			        ServiceReference<PortletFactory<?>> reference) {
				PortletFactory<?> portletFactory = context.getService(reference);
				if (StreamSupport.stream(spliterator(), false)
						.filter(c -> c.getClass()
								.equals(portletFactory.componentType()))
						.count() == 0) {
					Map<Object,Object> props = new HashMap<>();
					props.put(BundleContext.class, context);
					attach(portletFactory.create(channel(), props));
				}
				return portletFactory;
			}

			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
			 */
			@Override
			public void modifiedService(ServiceReference<PortletFactory<?>> reference,
			        PortletFactory<?> service) {
			}

			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
			 */
			@Override
			public void removedService(ServiceReference<PortletFactory<?>> reference,
			        PortletFactory<?> service) {
				for (ComponentType child: PortalComponentsCollector.this) {
					if (child.getClass().equals(service.componentType())) {
						Components.manager(child).detach();
					}
				}
			}
			
		});
		portletTracker.open();
	}


}
