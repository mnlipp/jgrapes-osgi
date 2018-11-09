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

package org.jgrapes.osgi.portlets.upnpbrowser;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.base.PortalSession;
import org.jgrapes.portal.base.PortalUtils;
import org.jgrapes.portal.base.Portlet.RenderMode;
import static org.jgrapes.portal.base.Portlet.RenderMode.DeleteablePreview;
import static org.jgrapes.portal.base.Portlet.RenderMode.View;
import org.jgrapes.portal.base.RenderSupport;
import org.jgrapes.portal.base.ResourceByInputStream;
import org.jgrapes.portal.base.ResourceNotModified;
import org.jgrapes.portal.base.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.base.events.AddPortletRequest;
import org.jgrapes.portal.base.events.AddPortletType;
import org.jgrapes.portal.base.events.DeletePortlet;
import org.jgrapes.portal.base.events.DeletePortletRequest;
import org.jgrapes.portal.base.events.NotifyPortletView;
import org.jgrapes.portal.base.events.PortalReady;
import org.jgrapes.portal.base.events.PortletResourceRequest;
import org.jgrapes.portal.base.events.RenderPortletRequest;
import org.jgrapes.portal.base.events.RenderPortletRequestBase;
import org.jgrapes.portal.base.freemarker.FreeMarkerPortlet;
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
 * A portlet for inspecting the services in an OSGi runtime.
 */
@SuppressWarnings({ "PMD.ExcessiveImports" })
public class UPnPBrowserPortlet extends FreeMarkerPortlet
        implements ServiceListener {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        DeleteablePreview, View);
    private final BundleContext context;

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     *            on by default and that {@link Manager#fire(Event, Channel...)}
     *            sends the event to
     */
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public UPnPBrowserPortlet(Channel componentChannel, BundleContext context,
            ServiceComponentRuntime scr) {
        super(componentChannel, true);
        this.context = context;
    }

    /**
     * On {@link PortalReady}, fire the {@link AddPortletType}.
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
    public void onPortalReady(PortalReady event, PortalSession channel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        ResourceBundle resourceBundle = resourceBundle(channel.locale());
        Reader deviceTemplate = new InputStreamReader(UPnPBrowserPortlet.class
            .getResourceAsStream("device-tree-template.html"));
        // Add portlet resources to page
        channel.respond(new AddPortletType(type())
            .setDisplayName(resourceBundle.getString("portletName"))
            .addScript(new ScriptResource()
                .setRequires(new String[] { "vuejs.org" })
                .setScriptUri(event.renderSupport().portletResource(
                    type(), "UPnPBrowser-functions.ftl.js")))
            .addScript(
                new ScriptResource()
                    .setScriptId("upnpbrowser-device-tree-template")
                    .setScriptType("text/x-template")
                    .loadScriptSource(deviceTemplate))
            .addCss(event.renderSupport(),
                PortalUtils.uriFromPath("UPnPBrowser-style.css"))
            .setInstantiable());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#generatePortletId()
     */
    @Override
    protected String generatePortletId() {
        return type() + "-" + super.generatePortletId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#modelFromSession
     */
    @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
    @Override
    protected <T extends Serializable> Optional<T> stateFromSession(
            Session session, String portletId, Class<T> type) {
        if (portletId.startsWith(type() + "-")) {
            return Optional.of((T) new UPnPBrowserModel(portletId));
        }
        return Optional.empty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doAddPortlet
     */
    @Override
    protected String doAddPortlet(AddPortletRequest event,
            PortalSession channel)
            throws Exception {
        UPnPBrowserModel portletModel
            = new UPnPBrowserModel(generatePortletId());
        renderPortlet(event, channel, portletModel);
        return portletModel.getPortletId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet
     */
    @Override
    protected void doRenderPortlet(RenderPortletRequest event,
            PortalSession channel, String portletId,
            Serializable retrievedState)
            throws Exception {
        UPnPBrowserModel portletModel = (UPnPBrowserModel) retrievedState;
        renderPortlet(event, channel, portletModel);
    }

    @SuppressWarnings({ "PMD.AvoidDuplicateLiterals",
        "PMD.DataflowAnomalyAnalysis", "unchecked" })
    private void renderPortlet(RenderPortletRequestBase<?> event,
            PortalSession channel,
            UPnPBrowserModel portletModel) throws TemplateNotFoundException,
            MalformedTemplateNameException, ParseException, IOException,
            InvalidSyntaxException {
        switch (event.renderMode()) {
        case Preview:
        case DeleteablePreview: {
            Template tpl = freemarkerConfig()
                .getTemplate("UPnPBrowser-preview.ftl.html");
            channel.respond(new RenderPortletFromTemplate(event,
                UPnPBrowserPortlet.class, portletModel.getPortletId(),
                tpl, fmModel(event, channel, portletModel))
                    .setRenderMode(DeleteablePreview).setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            List<Map<String, Object>> deviceInfos = Arrays.stream(
                context.getAllServiceReferences(UPnPDevice.class.getName(),
                    "(!(" + UPnPDevice.PARENT_UDN + "=*))"))
                .map(svc -> createDeviceInfo(context,
                    (ServiceReference<UPnPDevice>) svc, event.renderSupport()))
                .collect(Collectors.toList());
            channel.respond(new NotifyPortletView(type(),
                portletModel.getPortletId(), "deviceUpdates", deviceInfos,
                "preview", true));
            break;
        }
        case View: {
            Template tpl
                = freemarkerConfig().getTemplate("UPnPBrowser-view.ftl.html");
            channel.respond(new RenderPortletFromTemplate(event,
                UPnPBrowserPortlet.class, portletModel.getPortletId(),
                tpl, fmModel(event, channel, portletModel))
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<String, Map<String, Object>> deviceInfos = new HashMap<>();
            Arrays.stream(context
                .getAllServiceReferences(UPnPDevice.class.getName(), null))
                .map(svc -> createDeviceInfo(context,
                    (ServiceReference<UPnPDevice>) svc, event.renderSupport()))
                .forEach(devInfo -> deviceInfos.put((String) devInfo.get("udn"),
                    devInfo));
            channel.respond(new NotifyPortletView(type(),
                portletModel.getPortletId(), "deviceUpdates",
                treeify(deviceInfos), "view", true));
            break;
        }
        default:
            break;
        }
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
                result.put("iconUrl", PortalUtils.mergeQuery(
                    renderSupport.portletResource(type(), ""),
                    Components.mapOf("udn", (String) deviceRef
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
    protected void doGetResource(PortletResourceRequest event,
            IOSubchannel channel) {
        Map<String, List<String>> query
            = PortalUtils.queryAsMap(event.resourceUri());
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
    private void provideIcon(PortletResourceRequest event, UPnPDevice device) {
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
     * on all known portal session channels.
     *
     * @param event the event
     */
    @Override
    public void serviceChanged(ServiceEvent event) {
        // TODO
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet
     */
    @Override
    protected void doDeletePortlet(DeletePortletRequest event,
            PortalSession channel,
            String portletId, Serializable portletState) throws Exception {
        channel.respond(new DeletePortlet(portletId));
    }

    /**
     * The portlet's model.
     */
    @SuppressWarnings("serial")
    public class UPnPBrowserModel extends PortletBaseModel {

        /**
         * Instantiates a new service list model.
         *
         * @param portletId the portlet id
         */
        public UPnPBrowserModel(String portletId) {
            super(portletId);
        }

    }
}
