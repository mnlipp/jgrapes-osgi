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

package org.jgrapes.osgi.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static java.util.function.Predicate.not;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentFactory;
import org.jgrapes.core.events.Stop;
import org.jgrapes.util.ComponentProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * A component that collects all services from the OSGi service registry
 * which implement the {@link ComponentFactory} interface. It uses each 
 * to create one or more components that are then attached to the 
 * component collector instance.
 * 
 * Effectively, the component collector leverages OSGi's service layer
 * to modify the component tree at run-time.
 * 
 * This class uses {@link ComponentProvider#setFactories(ComponentFactory...)} 
 * and {@link ComponentProvider#setPinned(List)} for its implementation.
 * As it inherits from {@link ComponentProvider}, it automatically
 * supports the provisioning of additional components through
 * {@link ConfigurationUpdate} events. If this is not desired, invoke
 * {@link ComponentProvider#setComponentsEntry(String)} with `null` as
 * argument. 
 * 
 * @param <F> the component factory type
 */
@SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
    "PMD.DataflowAnomalyAnalysis" })
public class ComponentCollector<F extends ComponentFactory>
        extends ComponentProvider implements ServiceTrackerCustomizer<F, F> {

    private static final List<Map<Object, Object>> SINGLE_DEFAULT
        = Arrays.asList(Collections.emptyMap());

    private BundleContext context;
    @SuppressWarnings("PMD.SingularField")
    private ServiceTracker<F, F> serviceTracker;
    private Function<String, List<Map<Object, Object>>> configurator;

    /**
     * Creates a collector component that uses a {@link ServiceTracker} 
     * to monitor the addition and removal of component factories.
     * 
     * @see #addingService(ServiceReference)
     * @see #removedService(ServiceReference, ComponentFactory)
     *
     * @param componentChannel this component's channel
     * @param context the OSGi {@link BundleContext}
     * @param factoryCls the factory class
     * @param configurator the function that provides the pinned configurations
     */
    public ComponentCollector(
            Channel componentChannel, BundleContext context,
            Class<F> factoryCls,
            Function<String, List<Map<Object, Object>>> configurator) {
        super(componentChannel);
        this.context = context;
        this.configurator = configurator;
        serviceTracker = new ServiceTracker<>(context, factoryCls, this);
        serviceTracker.open();
    }

    /**
     * Utility constructor that uses each factory to create a single instance,
     * using an empty map as properties.
     *
     * @param componentChannel this component's channel
     * @param context the bundle context
     * @param factoryClass the factory class
     */
    public ComponentCollector(Channel componentChannel, BundleContext context,
            Class<F> factoryClass) {
        this(componentChannel, context, factoryClass, type -> SINGLE_DEFAULT);
    }

    /**
     * Whenever a new factory is added, it is used to create component 
     * instances with this component's channel. First, the `configurator`
     * passed to the constructor is invoked with the name of the class 
     * of the component to be created as argument. The list of maps 
     * returned is then added to the pinned components 
     * (see {@link ComponentProvider#setPinned(List)}). 
     * 
     * The map return from the `configurator` is automatically augmented
     * with an entry with key {@link BundleContext}.class and the bundle 
     * context passed to the constructor.
     *
     * @see ServiceTrackerCustomizer#addingService(ServiceReference)
     */
    @Override
    public F addingService(ServiceReference<F> reference) {
        F factory = context.getService(reference);
        if (factories().containsKey(factory.componentType().getName())) {
            // Factory for the new component type is already known.
            return factory;
        }

        // Add configuration to provider before adding new factory
        List<Map<?, ?>> newConfigs = Stream.concat(
            // filter existing configs to avoid duplicates
            pinned().stream().filter(not(c -> factory.componentType().getName()
                .equals(c.get(COMPONENT_TYPE)))),
            // add new configs
            configurator.apply(
                factory.componentType().getName()).stream().map(c -> {
                    // Received may be immutable
                    var newMap = new HashMap<>(c);
                    newMap.put(BundleContext.class, context);
                    if (!c.containsKey(COMPONENT_TYPE)) {
                        newMap.put(COMPONENT_TYPE,
                            factory.componentType().getName());
                    }
                    return newMap;
                }))
            .collect(Collectors.toList());
        setPinned(newConfigs);

        // Now add factory
        ComponentFactory[] updatedFactories
            = Stream.concat(factories().values().stream(), Stream.of(factory))
                .toArray(s -> new ComponentFactory[s]);
        setFactories(updatedFactories);
        return factory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService
     */
    @Override
    public void modifiedService(ServiceReference<F> reference, F service) {
        // Do nothing.
    }

    /**
     * Deletes all child components with the type produced by the factory that 
     * is removed. A (possibly) final {@link Stop} event is send to the
     * detached subtrees which may be used to deactivate bundles.
     * 
     * @see ServiceTrackerCustomizer#removedService(ServiceReference, Object)
     */
    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void removedService(ServiceReference<F> reference, F service) {
        // Remove factory.
        ComponentFactory[] updatedFactories = factories().values().stream()
            .filter(f -> !f.componentType().getName()
                .equals(service.componentType().getName()))
            .toArray(s -> new ComponentFactory[s]);
        setFactories(updatedFactories);

        // Remove configuration
        List<Map<?, ?>> updatedConfigs = pinned().stream()
            .filter(not(c -> service.componentType().getName()
                .equals(c.get(COMPONENT_TYPE))))
            .collect(Collectors.toList());
        setPinned(updatedConfigs);
    }
}
