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

package org.jgrapes.osgi.portlets.upnpbrowser;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.portal.base.PortalSession;
import org.jgrapes.portal.base.PortalWeblet;
import org.jgrapes.portal.base.Portlet.RenderMode;

import static org.jgrapes.portal.base.Portlet.RenderMode.DeleteablePreview;
import static org.jgrapes.portal.base.Portlet.RenderMode.View;

import org.jgrapes.portal.base.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.base.events.AddPortletRequest;
import org.jgrapes.portal.base.events.AddPortletType;
import org.jgrapes.portal.base.events.DeletePortlet;
import org.jgrapes.portal.base.events.DeletePortletRequest;
import org.jgrapes.portal.base.events.NotifyPortletView;
import org.jgrapes.portal.base.events.PortalReady;
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

/**
 * A portlet for inspecting the services in an OSGi runtime.
 */
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
        // Add portlet resources to page
        channel.respond(new AddPortletType(type())
            .setDisplayName(resourceBundle.getString("portletName"))
            .addScript(new ScriptResource()
                .setRequires(new String[] { "vuejs.org" })
                .setScriptUri(event.renderSupport().portletResource(
                    type(), "UPnPBrowser-functions.ftl.js")))
            .addCss(event.renderSupport(),
                PortalWeblet.uriFromPath("UPnPBrowser-style.css"))
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
    @SuppressWarnings("unchecked")
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
        "PMD.DataflowAnomalyAnalysis" })
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
            List<Map<String, Object>> serviceInfos = Arrays.stream(
                context.getAllServiceReferences(null,
                    "(&" + "(" + Constants.OBJECTCLASS
                        + "=" + UPnPDevice.class.getName() + ")"
                        + "(" + UPnPDevice.UDN + "=*)" + ")"))
                .map(svc -> createServiceInfo(svc))
                .collect(Collectors.toList());
            channel.respond(new NotifyPortletView(type(),
                portletModel.getPortletId(), "deviceUpdates", serviceInfos,
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
            List<Map<String, Object>> serviceInfos = Arrays.stream(
                context.getAllServiceReferences(UPnPDevice.class.toString(),
                    null))
                .map(svc -> createServiceInfo(svc))
                .collect(Collectors.toList());
            channel.respond(new NotifyPortletView(type(),
                portletModel.getPortletId(), "deviceUpdates", serviceInfos,
                "view", true));
            break;
        }
        default:
            break;
        }
    }

    @SuppressWarnings({ "PMD.NcssCount" })
    private Map<String, Object>
            createServiceInfo(ServiceReference<?> serviceRef) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> result = new HashMap<>();
        result.put("serviceId", serviceRef.getProperty(Constants.SERVICE_ID));
        String[] props = serviceRef.getPropertyKeys();
        result.put("friendlyName",
            serviceRef.getProperty(UPnPDevice.FRIENDLY_NAME));
        return result;
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
