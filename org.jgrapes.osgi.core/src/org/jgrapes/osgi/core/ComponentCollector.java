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

package org.jgrapes.osgi.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentFactory;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * An advanced version of the basic {@link ComponentCollector} that is based on 
 * the OSGi registry and not on the {@link ServiceLoader}.
 */
public class ComponentCollector<F extends ComponentFactory> extends Component 
	implements ServiceTrackerCustomizer<F, F> {

	private BundleContext context;
	private ServiceTracker<F, F> serviceTracker;
	private Function<String,List<Map<Object,Object>>> matcher;
	
	/**
	 * Creates a collector component that uses the {@link ServiceTracker} to
	 * monitor the addition and removal of component factories.
	 *
	 * @param componentChannel this component's channel
	 * @param context the OSGi {@link BundleContext}
	 * @param factoryCls the factory class
	 * @param matcher the matcher function
	 */
	public ComponentCollector(
			Channel componentChannel, BundleContext context, Class<F> factoryCls, 
			Function<String,List<Map<Object,Object>>> matcher) {
		super(componentChannel);
		this.context = context;
		this.matcher = matcher;
		serviceTracker = new ServiceTracker<>(context, factoryCls, this);
		serviceTracker.open();
	}

	private static List<Map<Object,Object>> SINGLE_DEFAULT 
		= Arrays.asList(Collections.emptyMap());

	/**
	 * Utility constructor that uses each factory to create a single
	 * instance, using an empty map as properties.
	 * 
	 * @param factoryClass the factory class
	 * @param componentChannel this component's channel
	 */
	public ComponentCollector(Channel componentChannel, BundleContext context,
			Class<F> factoryClass) {
		this(componentChannel, context, factoryClass, type -> SINGLE_DEFAULT);
	}
	
	/**
	 * Whenever a new factory is added, it is used to create 
	 * component instances with this component's channel. First, the 
	 * `matcher` function passed to the constructor is invoked 
	 * with the name of the class of the component to be created 
	 * as argument. The list of maps returned is used to create 
	 * components, passing each element in the list 
	 * as parameter to {@link ComponentFactory#create(Channel, Map)}.
	 * The map return from the `matcher` is automatically augmented
	 * with an entry with key {@link BundleContext}.class and
	 * the bundle context passed to the constructor.
	 *
	 * @see ServiceTrackerCustomizer#addingService(ServiceReference)
	 */
	@Override
	public F addingService(ServiceReference<F> reference) {
		F factory = context.getService(reference);
		if (StreamSupport.stream(spliterator(), false)
				.filter(c -> c.getClass().equals(factory.componentType()))
				.count() == 0) {
			List<Map<Object,Object>> configs = matcher.apply(
					factory.componentType().getName());
			for (Map<?,?> config: configs) {
				Map<Object,Object> props = new HashMap<>(config);
				props.put(BundleContext.class, context);
				factory.create(channel(), props).ifPresent(
						component -> attach(component));
			}
		}
		return factory;
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService
	 */
	@Override
	public void modifiedService(ServiceReference<F> reference, F service) {
	}

	/**
	 * Removes all child component with the type produced by the
	 * factory that is removed.
	 * 
	 * @see ServiceTrackerCustomizer#removedService(ServiceReference, Object)
	 */
	@Override
	public void removedService(ServiceReference<F> reference, F service) {
		for (ComponentType child: this) {
			if (child.getClass().equals(service.componentType())) {
				Components.manager(child).detach();
			}
		}
	}

}
