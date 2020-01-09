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

package org.jgrapes.osgi.portlets.bundles;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.portal.base.AbstractPortlet;
import org.jgrapes.portal.base.PortalSession;
import org.jgrapes.portal.base.PortalUtils;

import static org.jgrapes.portal.base.Portlet.RenderMode;

import org.jgrapes.portal.base.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.base.events.AddPortletRequest;
import org.jgrapes.portal.base.events.AddPortletType;
import org.jgrapes.portal.base.events.DeletePortlet;
import org.jgrapes.portal.base.events.DeletePortletRequest;
import org.jgrapes.portal.base.events.NotifyPortletModel;
import org.jgrapes.portal.base.events.NotifyPortletView;
import org.jgrapes.portal.base.events.PortalReady;
import org.jgrapes.portal.base.events.RenderPortletRequest;
import org.jgrapes.portal.base.events.RenderPortletRequestBase;
import org.jgrapes.portal.base.freemarker.FreeMarkerPortlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;

/**
 * 
 */
public class BundleListPortlet
        extends FreeMarkerPortlet<BundleListPortlet.BundleListModel>
        implements BundleListener {

    private static final Logger logger
        = Logger.getLogger(BundleListPortlet.class.getName());

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.DeleteablePreview, RenderMode.View);
    private final BundleContext context;

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     *            on by default and that {@link Manager#fire(Event, Channel...)}
     *            sends the event to
     */
    public BundleListPortlet(Channel componentChannel, BundleContext context,
            Map<Object, Object> properties) {
        super(componentChannel);
        this.context = context;
        context.addBundleListener(this);
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
        // Add portlet resources to page
        channel.respond(new AddPortletType(type())
            .setDisplayNames(
                displayNames(channel.supportedLocales(), "portletName"))
            .addScript(new ScriptResource()
                .setRequires(new String[] { "vuejs.org" })
                .setScriptUri(event.renderSupport().portletResource(
                    type(), "Bundles-functions.ftl.js")))
            .addCss(event.renderSupport(),
                PortalUtils.uriFromPath("Bundles-style.css")));
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
    @Override
    protected Optional<BundleListModel> stateFromSession(
            Session session, String portletId) {
        if (portletId.startsWith(type() + "-")) {
            return Optional.of(new BundleListModel(portletId));
        }
        return Optional.empty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doAddPortlet
     */
    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    protected String doAddPortlet(AddPortletRequest event,
            PortalSession channel)
            throws Exception {
        BundleListModel portletModel = new BundleListModel(generatePortletId());
        Template tpl
            = freemarkerConfig().getTemplate("Bundles-preview.ftl.html");
        channel.respond(new RenderPortletFromTemplate(event,
            BundleListPortlet.class, portletModel.getPortletId(),
            tpl, fmModel(event, channel, portletModel))
                .setRenderMode(RenderMode.DeleteablePreview)
                .setSupportedModes(MODES)
                .setForeground(true));
        List<Map<String, Object>> bundleInfos
            = Arrays.stream(context.getBundles())
                .map(bndl -> createBundleInfo(bndl, channel.locale()))
                .collect(Collectors.toList());
        channel.respond(new NotifyPortletView(type(),
            portletModel.getPortletId(), "bundleUpdates", bundleInfos,
            "preview", true));
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
            BundleListModel portletModel)
            throws Exception {
        renderPortlet(event, channel, portletModel);
    }

    private void renderPortlet(RenderPortletRequestBase<?> event,
            PortalSession channel,
            BundleListModel portletModel) throws TemplateNotFoundException,
            MalformedTemplateNameException, ParseException, IOException {
        if (event.renderPreview()) {
            Template tpl
                = freemarkerConfig().getTemplate("Bundles-preview.ftl.html");
            channel.respond(new RenderPortletFromTemplate(event,
                BundleListPortlet.class, portletModel.getPortletId(),
                tpl, fmModel(event, channel, portletModel))
                    .setRenderMode(RenderMode.DeleteablePreview)
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            List<Map<String, Object>> bundleInfos
                = Arrays.stream(context.getBundles())
                    .map(bndl -> createBundleInfo(bndl, channel.locale()))
                    .collect(Collectors.toList());
            channel.respond(new NotifyPortletView(type(),
                portletModel.getPortletId(), "bundleUpdates", bundleInfos,
                "preview", true));
        }
        if (event.renderModes().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("Bundles-view.ftl.html");
            channel.respond(new RenderPortletFromTemplate(event,
                BundleListPortlet.class, portletModel.getPortletId(),
                tpl, fmModel(event, channel, portletModel))
                    .setRenderMode(RenderMode.View).setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            List<Map<String, Object>> bundleInfos
                = Arrays.stream(context.getBundles())
                    .map(bndl -> createBundleInfo(bndl, channel.locale()))
                    .collect(Collectors.toList());
            channel.respond(new NotifyPortletView(type(),
                portletModel.getPortletId(), "bundleUpdates", bundleInfos,
                "view", true));
        }
    }

    private Map<String, Object> createBundleInfo(Bundle bundle, Locale locale) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> result = new HashMap<>();
        result.put("id", bundle.getBundleId());
        result.put("name",
            Optional.ofNullable(bundle.getHeaders(locale.toString())
                .get("Bundle-Name")).orElse(bundle.getSymbolicName()));
        result.put("symbolicName", bundle.getSymbolicName());
        result.put("version", bundle.getVersion().toString());
        result.put("category",
            Optional.ofNullable(bundle.getHeaders(locale.toString())
                .get("Bundle-Category")).orElse(""));
        ResourceBundle resources = resourceBundle(locale);
        result.put("state",
            resources.getString("bundleState_" + bundle.getState()));
        result.put("startable", false);
        result.put("stoppable", false);
        if ((bundle.getState()
            & (Bundle.RESOLVED | Bundle.INSTALLED | Bundle.ACTIVE)) != 0) {
            boolean isFragment = (bundle.adapt(BundleRevision.class).getTypes()
                & BundleRevision.TYPE_FRAGMENT) != 0;
            result.put("startable", !isFragment
                && (bundle.getState() == Bundle.INSTALLED
                    || bundle.getState() == Bundle.RESOLVED));
            result.put("stoppable",
                !isFragment && bundle.getState() == Bundle.ACTIVE);
        }
        result.put("uninstallable", (bundle.getState()
            & (Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE)) != 0);
        result.put("uninstalled", bundle.getState() == Bundle.UNINSTALLED);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet
     */
    @Override
    protected void doDeletePortlet(DeletePortletRequest event,
            PortalSession channel, String portletId,
            BundleListModel retrievedState) throws Exception {
        channel.respond(new DeletePortlet(portletId));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doNotifyPortletModel
     */
    @Override
    protected void doNotifyPortletModel(NotifyPortletModel event,
            PortalSession channel, BundleListModel portletState)
            throws Exception {
        event.stop();
        Bundle bundle = context.getBundle(event.params().asInt(0));
        if (bundle == null) {
            return;
        }
        try {
            switch (event.method()) {
            case "stop":
                bundle.stop();
                break;
            case "start":
                bundle.start();
                break;
            case "refresh":
                break;
            case "update":
                bundle.update();
                break;
            case "uninstall":
                bundle.uninstall();
                break;
            case "sendDetails":
                sendBundleDetails(event.portletId(), channel, bundle);
                break;
            default:// ignore
                break;
            }
        } catch (BundleException e) {
            // ignore
            logger.log(Level.WARNING, "Cannot update bundle state", e);
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void sendBundleDetails(String portletId, PortalSession channel,
            Bundle bundle) {
        Locale locale = channel.locale();
        ResourceBundle resources = resourceBundle(locale);
        List<Object> data = new ArrayList<>();
        data.add(new Object[] { resources.getString("bundleSymbolicName"),
            bundle.getSymbolicName() });
        data.add(new Object[] { resources.getString("bundleVersion"),
            bundle.getVersion().toString() });
        data.add(new Object[] { resources.getString("bundleLocation"),
            bundle.getLocation().replace(".", ".&#x200b;") });
        data.add(new Object[] { resources.getString("bundleLastModification"),
            Instant.ofEpochMilli(bundle.getLastModified()).toString(),
            "dateTime" });
        data.add(new Object[] { resources.getString("bundleStartLevel"),
            bundle.adapt(BundleStartLevel.class).getStartLevel() });
        Dictionary<String, String> dict = bundle.getHeaders(locale.toString());
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, String> headers = new TreeMap<>();
        for (Enumeration<String> e = dict.keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            headers.put(key, dict.get(key));
        }
        List<Object> headerList = new ArrayList<>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            headerList.add(new Object[] { e.getKey(),
                e.getKey().contains("Package")
                    ? e.getValue().replace(".", ".&#x200b;")
                    : e.getValue() });
        }
        data.add(
            new Object[] { resources.getString("manifestHeaders"), headerList,
                "table" });
        channel.respond(new NotifyPortletView(type(),
            portletId, "bundleDetails", bundle.getBundleId(), data));
    }

    /**
     * Translates the OSGi {@link BundleEvent} to a JGrapes event and fires it
     * on all known portal session channels.
     *
     * @param event the event
     */
    @Override
    public void bundleChanged(BundleEvent event) {
        fire(new BundleChanged(event), trackedSessions());
    }

    /**
     * Handles a {@link BundleChanged} event by updating the information in the
     * portal sessions.
     *
     * @param event the event
     */
    @Handler
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void onBundleChanged(BundleChanged event,
            PortalSession portalSession) {
        for (String portletId : portletIds(portalSession)) {
            portalSession.respond(new NotifyPortletView(type(), portletId,
                "bundleUpdates",
                (Object) new Object[] { createBundleInfo(
                    event.bundleEvent().getBundle(), portalSession.locale()) },
                "*", false));
        }
    }

    /**
     * Wraps an OSGi {@link BundleEvent}.
     */
    public static class BundleChanged extends Event<Void> {
        private final BundleEvent bundleEvent;

        /**
         * Instantiates a new event.
         *
         * @param bundleEvent the OSGi bundle event
         */
        public BundleChanged(BundleEvent bundleEvent) {
            this.bundleEvent = bundleEvent;
        }

        /**
         * Return the OSGi bundle event.
         *
         * @return the bundle event
         */
        public BundleEvent bundleEvent() {
            return bundleEvent;
        }

    }

    /**
     * The bundle's model.
     */
    @SuppressWarnings("serial")
    public class BundleListModel extends AbstractPortlet.PortletBaseModel {

        /**
         * Instantiates a new bundle list model.
         *
         * @param portletId the portlet id
         */
        public BundleListModel(String portletId) {
            super(portletId);
        }

        /**
         * Return the bundles.
         *
         * @return the bundle[]
         */
        public Bundle[] bundles() {
            return context.getBundles();
        }

    }
}
