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

package org.jgrapes.osgi.webconlet.bundles;

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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
import org.jgrapes.webconsole.base.AbstractConlet;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.events.SetLocale;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;
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
public class BundleListConlet
        extends FreeMarkerConlet<BundleListConlet.BundleListModel>
        implements BundleListener {

    private static final Logger LOG
        = Logger.getLogger(BundleListConlet.class.getName());

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
    public BundleListConlet(Channel componentChannel, BundleContext context,
            Map<Object, Object> properties) {
        super(componentChannel);
        this.context = context;
        context.addBundleListener(this);
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
                    type(), "Bundles-functions.ftl.js"))
                .setScriptType("module"))
            .addCss(event.renderSupport(),
                WebConsoleUtils.uriFromPath("Bundles-style.css")));
    }

    @Override
    protected String generateConletId() {
        return type() + "-" + super.generateConletId();
    }

    @Override
    protected Optional<BundleListModel> stateFromSession(
            Session session, String conletId) {
        if (conletId.startsWith(type() + "-")) {
            return Optional.of(new BundleListModel(conletId));
        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    protected ConletTrackingInfo doAddConlet(AddConletRequest event,
            ConsoleSession channel)
            throws Exception {
        BundleListModel conletModel = new BundleListModel(generateConletId());
        Template tpl
            = freemarkerConfig().getTemplate("Bundles-preview.ftl.html");
        channel.respond(new RenderConletFromTemplate(event,
            BundleListConlet.class, conletModel.getConletId(),
            tpl, fmModel(event, channel, conletModel))
                .setRenderAs(RenderMode.Preview)
                .setSupportedModes(MODES));
        List<Map<String, Object>> bundleInfos
            = Arrays.stream(context.getBundles())
                .map(bndl -> createBundleInfo(bndl, channel.locale()))
                .collect(Collectors.toList());
        channel.respond(new NotifyConletView(type(),
            conletModel.getConletId(), "bundleUpdates", bundleInfos,
            "preview", true));
        return new ConletTrackingInfo(conletModel.getConletId())
            .addModes(RenderMode.asSet(RenderMode.Preview));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequest event,
            ConsoleSession channel, String conletId,
            BundleListModel conletModel)
            throws Exception {
        return renderConlet(event, channel, conletModel);
    }

    private Set<RenderMode> renderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel,
            BundleListModel conletModel) throws TemplateNotFoundException,
            MalformedTemplateNameException, ParseException, IOException {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl
                = freemarkerConfig().getTemplate("Bundles-preview.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                BundleListConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, channel, conletModel))
                    .setRenderAs(
                        RenderMode.Preview.addModifiers(event.renderAs()))
                    .setSupportedModes(MODES));
            List<Map<String, Object>> bundleInfos
                = Arrays.stream(context.getBundles())
                    .map(bndl -> createBundleInfo(bndl, channel.locale()))
                    .collect(Collectors.toList());
            channel.respond(new NotifyConletView(type(),
                conletModel.getConletId(), "bundleUpdates", bundleInfos,
                "preview", true));
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("Bundles-view.ftl.html");
            channel.respond(new RenderConletFromTemplate(event,
                BundleListConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, channel, conletModel))
                    .setRenderAs(
                        RenderMode.View.addModifiers(event.renderAs())));
            List<Map<String, Object>> bundleInfos
                = Arrays.stream(context.getBundles())
                    .map(bndl -> createBundleInfo(bndl, channel.locale()))
                    .collect(Collectors.toList());
            channel.respond(new NotifyConletView(type(),
                conletModel.getConletId(), "bundleUpdates", bundleInfos,
                "view", true));
            renderedAs.add(RenderMode.View);
        }
        return renderedAs;
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
        result.put("state", "bundleState_" + bundle.getState());
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

    @Override
    protected void doNotifyConletModel(NotifyConletModel event,
            ConsoleSession channel, BundleListModel conletState)
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
                sendBundleDetails(event.conletId(), channel, bundle);
                break;
            default:// ignore
                break;
            }
        } catch (BundleException e) {
            // ignore
            LOG.log(Level.WARNING, "Cannot update bundle state", e);
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void sendBundleDetails(String conletId, ConsoleSession channel,
            Bundle bundle) {
        Locale locale = channel.locale();
        List<Object> data = new ArrayList<>();
        data.add(
            new Object[] { "bundleSymbolicName", bundle.getSymbolicName() });
        data.add(
            new Object[] { "bundleVersion", bundle.getVersion().toString() });
        data.add(new Object[] { "bundleLocation",
            bundle.getLocation().replace(".", ".&#x200b;") });
        data.add(new Object[] { "bundleLastModification",
            Instant.ofEpochMilli(bundle.getLastModified()).toString(),
            "dateTime" });
        data.add(new Object[] { "bundleStartLevel",
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
        data.add(new Object[] { "manifestHeaders", headerList, "table" });
        channel.respond(new NotifyConletView(type(),
            conletId, "bundleDetails", bundle.getBundleId(), data));
    }

    /**
     * Translates the OSGi {@link BundleEvent} to a JGrapes event and fires it
     * on all known console session channels.
     *
     * @param event the event
     */
    @Override
    public void bundleChanged(BundleEvent event) {
        fire(new BundleChanged(event), trackedSessions());
    }

    /**
     * Handles a {@link BundleChanged} event by updating the information in the
     * console sessions.
     *
     * @param event the event
     */
    @Handler
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void onBundleChanged(BundleChanged event,
            ConsoleSession consoleSession) {
        for (String conletId : conletIds(consoleSession)) {
            consoleSession.respond(new NotifyConletView(type(), conletId,
                "bundleUpdates",
                (Object) new Object[] { createBundleInfo(
                    event.bundleEvent().getBundle(), consoleSession.locale()) },
                "*", false));
        }
    }

    @Override
    protected boolean doSetLocale(SetLocale event, ConsoleSession channel,
            String conletId) throws Exception {
        return true;
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
    public class BundleListModel extends AbstractConlet.ConletBaseModel {

        /**
         * Instantiates a new bundle list model.
         *
         * @param conletId the web console component id
         */
        public BundleListModel(String conletId) {
            super(conletId);
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
