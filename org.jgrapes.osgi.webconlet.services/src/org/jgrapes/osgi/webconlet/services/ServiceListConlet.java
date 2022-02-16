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

package org.jgrapes.osgi.webconlet.services;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

/**
 * A conlet for inspecting the services in an OSGi runtime.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ServiceListConlet
        extends FreeMarkerConlet<Serializable> implements ServiceListener {

    private final ServiceComponentRuntime scr;
    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.Preview, RenderMode.View);
    private final BundleContext context;

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     *            on by default and that {@link Manager#fire(Event, Channel...)}
     *            sends the event to
     */
    public ServiceListConlet(Channel componentChannel, BundleContext context,
            ServiceComponentRuntime scr) {
        super(componentChannel);
        this.context = context;
        this.scr = scr;
        context.addServiceListener(this);
    }

    /**
     * On {@link ConsoleReady}, fire the {@link AddConletType}.
     *
     * @param event the event
     * @param channel the channel
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name
     *             exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleSession channel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add conlet resources to page
        channel.respond(new AddConletType(type())
            .setDisplayNames(
                localizations(channel.supportedLocales(), "conletName"))
            .addScript(new ScriptResource()
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "Services-functions.ftl.js"))
                .setScriptType("module"))
            .addCss(event.renderSupport(),
                WebConsoleUtils.uriFromPath("Services-style.css")));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel, String conletId, Serializable conletState)
            throws Exception {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl
                = freemarkerConfig().getTemplate("Services-preview.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                type(), conletId, tpl,
                fmModel(event, channel, conletId, conletState))
                    .setRenderAs(
                        RenderMode.Preview.addModifiers(event.renderAs()))
                    .setSupportedModes(MODES));
            List<Map<String, Object>> serviceInfos = Arrays.stream(
                context.getAllServiceReferences(null, null))
                .map(svc -> createServiceInfo(svc, channel.locale()))
                .collect(Collectors.toList());
            channel.respond(new NotifyConletView(type(),
                conletId, "serviceUpdates", serviceInfos, "preview", true));
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("Services-view.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                type(), conletId, tpl,
                fmModel(event, channel, conletId, conletState))
                    .setRenderAs(
                        RenderMode.View.addModifiers(event.renderAs())));
            List<Map<String, Object>> serviceInfos = Arrays.stream(
                context.getAllServiceReferences(null, null))
                .map(svc -> createServiceInfo(svc, channel.locale()))
                .collect(Collectors.toList());
            channel.respond(new NotifyConletView(type(),
                conletId, "serviceUpdates", serviceInfos, "view", true));
            renderedAs.add(RenderMode.View);
        }
        return renderedAs;
    }

    @SuppressWarnings({ "PMD.NcssCount", "PMD.ConfusingTernary",
        "PMD.NPathComplexity", "PMD.AssignmentInOperand",
        "PMD.CognitiveComplexity" })
    private Map<String, Object>
            createServiceInfo(ServiceReference<?> serviceRef, Locale locale) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> result = new HashMap<>();
        result.put("id", serviceRef.getProperty(Constants.SERVICE_ID));
        String[] interfaces
            = (String[]) serviceRef.getProperty(Constants.OBJECTCLASS);
        result.put("type", String.join(", ", interfaces));
        Long bundleId
            = (Long) serviceRef.getProperty(Constants.SERVICE_BUNDLEID);
        result.put("bundleId", bundleId.toString());
        Bundle bundle = context.getBundle(bundleId);
        if (bundle == null) {
            result.put("bundleName", "");
        } else {
            result.put("bundleName", Optional
                .ofNullable(bundle.getHeaders(locale.toString())
                    .get("Bundle-Name"))
                .orElse(bundle.getSymbolicName()));
        }
        String scope;
        switch ((String) serviceRef.getProperty(Constants.SERVICE_SCOPE)) {
        case Constants.SCOPE_BUNDLE:
            scope = "serviceScopeBundle";
            break;
        case Constants.SCOPE_PROTOTYPE:
            scope = "serviceScopePrototype";
            break;
        case Constants.SCOPE_SINGLETON:
            scope = "serviceScopeSingleton";
            break;
        default:
            scope = "";
            break;
        }
        result.put("scope", scope);
        Integer ranking
            = (Integer) serviceRef.getProperty(Constants.SERVICE_RANKING);
        result.put("ranking", ranking == null ? "" : ranking.toString());
        String componentName
            = (String) serviceRef.getProperty("component.name");
        ComponentDescriptionDTO dto;
        if (componentName != null && bundle != null
            && (dto = scr.getComponentDescriptionDTO(bundle,
                componentName)) != null) {
            if (dto.scope != null) {
                result.put("dsScope", "serviceScope"
                    + dto.scope.substring(0, 1).toUpperCase(Locale.US)
                    + dto.scope.substring(1));
            }
            result.put("implementationClass", dto.implementationClass);
        } else {
            Object service = context.getService(serviceRef);
            if (service != null) {
                result.put("implementationClass", service.getClass().getName());
                context.ungetService(serviceRef);
            } else {
                result.put("implementationClass", "");
            }
        }
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> properties = new HashMap<>();
        for (String property : serviceRef.getPropertyKeys()) {
            properties.put(property, serviceRef.getProperty(property));
        }
        result.put("properties", properties);
        if (serviceRef.getUsingBundles() != null) {
            List<String> using = new ArrayList<>();
            for (Bundle bdl : serviceRef.getUsingBundles()) {
                using
                    .add(
                        bdl.getSymbolicName() + " (" + bdl.getBundleId() + ")");
            }
            result.put("usingBundles", using);
        }
        return result;
    }

    /**
     * Translates the OSGi {@link ServiceEvent} to a JGrapes event and fires it
     * on all known console session channels.
     *
     * @param event the event
     */
    @Override
    public void serviceChanged(ServiceEvent event) {
        fire(new ServiceChanged(event), trackedSessions());
    }

    /**
     * Handles a {@link ServiceChanged} event by updating the information in the
     * console sessions.
     *
     * @param event the event
     */
    @Handler
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.DataflowAnomalyAnalysis" })
    public void onServiceChanged(ServiceChanged event,
            ConsoleSession consoleSession) {
        Map<String, Object> info = createServiceInfo(
            event.serviceEvent().getServiceReference(),
            consoleSession.locale());
        if (event.serviceEvent().getType() == ServiceEvent.UNREGISTERING) {
            info.put("updateType", "unregistering");
        }
        for (String conletId : conletIds(consoleSession)) {
            consoleSession.respond(new NotifyConletView(
                type(), conletId, "serviceUpdates",
                (Object) new Object[] { info }, "*", false));
        }
    }

    /**
     * Wraps an OSGi {@link ServiceEvent}.
     */
    public static class ServiceChanged extends Event<Void> {
        private final ServiceEvent serviceEvent;

        /**
         * Instantiates a new event.
         *
         * @param serviceEvent the service event
         */
        public ServiceChanged(ServiceEvent serviceEvent) {
            this.serviceEvent = serviceEvent;
        }

        public ServiceEvent serviceEvent() {
            return serviceEvent;
        }
    }
}
