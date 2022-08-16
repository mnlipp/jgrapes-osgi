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

package org.jgrapes.osgi.webconlet.upnpbrowser;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConletBaseModel;
import org.jgrapes.webconsole.base.ConsoleConnection;
import org.jgrapes.webconsole.base.RenderSupport;
import org.jgrapes.webconsole.base.ResourceByInputStream;
import org.jgrapes.webconsole.base.ResourceNotModified;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConletResourceRequest;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConlet;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;

/**
 * A conlet for inspecting the services in an OSGi runtime.
 */
@SuppressWarnings({ "PMD.ExcessiveImports" })
public class UPnPBrowserConlet
        extends FreeMarkerConlet<UPnPBrowserConlet.UPnPBrowserModel>
        implements ServiceListener {

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
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public UPnPBrowserConlet(Channel componentChannel, BundleContext context,
            ServiceComponentRuntime scr) {
        super(componentChannel);
        this.context = context;
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
    public void onConsoleReady(ConsoleReady event, ConsoleConnection channel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        @SuppressWarnings("PMD.CloseResource")
        Reader deviceTemplate = new InputStreamReader(UPnPBrowserConlet.class
            .getResourceAsStream("device-tree-template.html"));
        // Add conlet resources to page
        channel.respond(new AddConletType(type())
            .addRenderMode(RenderMode.Preview).setDisplayNames(
                localizations(channel.supportedLocales(), "conletName"))
            .addScript(new ScriptResource()
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "UPnPBrowser-functions.ftl.js"))
                .setScriptType("module"))
            .addScript(
                new ScriptResource()
                    .setScriptId("upnpbrowser-device-tree-template")
                    .setScriptType("text/x-template")
                    .loadScriptSource(deviceTemplate))
            .addCss(event.renderSupport(),
                WebConsoleUtils.uriFromPath("UPnPBrowser-style.css")));
    }

    @Override
    protected Optional<UPnPBrowserModel> createNewState(AddConletRequest event,
            ConsoleConnection session, String conletId) throws Exception {
        return Optional.of(new UPnPBrowserModel(conletId));
    }

    @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleConnection channel, String conletId,
            UPnPBrowserModel conletState) throws Exception {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl = freemarkerConfig()
                .getTemplate("UPnPBrowser-preview.ftl.html");
            channel.respond(new RenderConlet(type(), conletId,
                processTemplate(event, tpl,
                    fmModel(event, channel, conletId, conletState)))
                        .setRenderAs(
                            RenderMode.Preview.addModifiers(event.renderAs()))
                        .setSupportedModes(MODES));
            List<Map<String, Object>> deviceInfos = Arrays.stream(
                context.getAllServiceReferences(UPnPDevice.class.getName(),
                    "(!(" + UPnPDevice.PARENT_UDN + "=*))"))
                .map(svc -> createDeviceInfo(context,
                    (ServiceReference<UPnPDevice>) svc, event.renderSupport()))
                .collect(Collectors.toList());
            channel.respond(new NotifyConletView(type(),
                conletId, "deviceUpdates", deviceInfos, "preview", true));
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("UPnPBrowser-view.ftl.html");
            channel.respond(new RenderConlet(type(), conletId,
                processTemplate(event, tpl,
                    fmModel(event, channel, conletId, conletState)))
                        .setRenderAs(
                            RenderMode.View.addModifiers(event.renderAs()))
                        .setSupportedModes(MODES));
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<String, Map<String, Object>> deviceInfos = new HashMap<>();
            Arrays.stream(context
                .getAllServiceReferences(UPnPDevice.class.getName(), null))
                .map(svc -> createDeviceInfo(context,
                    (ServiceReference<UPnPDevice>) svc, event.renderSupport()))
                .forEach(devInfo -> deviceInfos.put((String) devInfo.get("udn"),
                    devInfo));
            channel.respond(new NotifyConletView(type(),
                conletId, "deviceUpdates", treeify(deviceInfos), "view", true));
            renderedAs.add(RenderMode.View);
        }
        return renderedAs;
    }

    @SuppressWarnings({ "PMD.NcssCount", "PMD.AvoidDuplicateLiterals" })
    private Map<String, Object> createDeviceInfo(BundleContext context,
            ServiceReference<UPnPDevice> deviceRef,
            RenderSupport renderSupport) {
        UPnPDevice device = context.getService(deviceRef);
        if (device == null) {
            return null;
        }
        try {
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<String, Object> result = new HashMap<>();
            result.put("udn", (String) deviceRef.getProperty(UPnPDevice.UDN));
            result.computeIfAbsent("parentUdn",
                k -> (String) deviceRef.getProperty(UPnPDevice.PARENT_UDN));
            result.put("friendlyName",
                deviceRef.getProperty(UPnPDevice.FRIENDLY_NAME));
            if (device.getIcons(null) != null) {
                result.put("iconUrl", WebConsoleUtils.mergeQuery(
                    renderSupport.conletResource(type(), ""),
                    Map.of("udn", (String) deviceRef
                        .getProperty(UPnPDevice.UDN), "resource", "icon"))
                    .toASCIIString());
            }
            return result;
        } finally {
            context.ungetService(deviceRef);
        }
    }

    @SuppressWarnings({ "unchecked", "PMD.AvoidInstantiatingObjectsInLoops" })
    private List<Map<String, Object>>
            treeify(Map<String, Map<String, Object>> deviceInfos) {
        for (Map.Entry<String, Map<String, Object>> e : deviceInfos
            .entrySet()) {
            Optional.ofNullable(e.getValue().get("parentUdn")).ifPresent(
                parentUdn -> ((List<Map<String, Object>>) deviceInfos
                    .get(parentUdn).computeIfAbsent("childDevices",
                        k -> new ArrayList<Map<String, Object>>()))
                            .add(e.getValue()));
        }
        return deviceInfos.values().stream()
            .filter(deviceInfo -> !deviceInfo.containsKey("parentUdn"))
            .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis" })
    protected void doGetResource(ConletResourceRequest event,
            IOSubchannel channel) {
        Map<String, List<String>> query
            = WebConsoleUtils.queryAsMap(event.resourceUri());
        if (!query.containsKey("udn")) {
            super.doGetResource(event, channel);
            return;
        }
        try {
            Arrays.stream(context.getAllServiceReferences(null,
                String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS,
                    UPnPDevice.class.getName(), UPnPDevice.UDN,
                    query.get("udn").get(0))))
                .findFirst().ifPresent(deviceRef -> {
                    @SuppressWarnings("unchecked")
                    UPnPDevice device = context
                        .getService((ServiceReference<UPnPDevice>) deviceRef);
                    if (query.getOrDefault("resource", Collections.emptyList())
                        .contains("icon")) {
                        provideIcon(event, device);
                    }
                });
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings({ "PMD.EmptyCatchBlock", "PMD.DataflowAnomalyAnalysis" })
    private void provideIcon(ConletResourceRequest event, UPnPDevice device) {
        UPnPIcon[] icons = device
            .getIcons(event.session().locale().toLanguageTag());
        if (icons == null) {
            icons = device.getIcons(null);
        }
        if (icons == null) {
            return;
        }
        Arrays.sort(icons,
            Comparator.comparingInt(UPnPIcon::getHeight).reversed());
        UPnPIcon icon = icons[0];
        try {
            if (event.ifModifiedSince().isPresent()) {
                event.setResult(new ResourceNotModified(event, Instant.now(),
                    365 * 24 * 3600));
            } else {
                MediaType mediaType = null;
                if (icon.getMimeType() != null
                    && !icon.getMimeType().isEmpty()) {
                    mediaType
                        = Converters.MEDIA_TYPE
                            .fromFieldValue(icon.getMimeType());
                }
                event.setResult(new ResourceByInputStream(event,
                    icon.getInputStream(), mediaType, Instant.now(),
                    365 * 24 * 3600));
            }
            event.stop();
        } catch (IOException | java.text.ParseException e) {
            // Handle as if no match
        }
    }

    /**
     * Translates the OSGi {@link ServiceEvent} to a JGrapes event and fires it
     * on all known console session channels.
     *
     * @param event the event
     */
    @Override
    public void serviceChanged(ServiceEvent event) {
        // TODO
    }

    /**
     * The conlet's model.
     */
    @SuppressWarnings("serial")
    public class UPnPBrowserModel extends ConletBaseModel {

        /**
         * Instantiates a new service list model.
         *
         * @param conletId the conlet id
         */
        public UPnPBrowserModel(String conletId) {
            super(conletId);
        }

    }
}
