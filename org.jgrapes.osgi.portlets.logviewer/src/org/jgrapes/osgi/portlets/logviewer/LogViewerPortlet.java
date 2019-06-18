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

package org.jgrapes.osgi.portlets.logviewer;

import de.mnl.osgi.coreutils.ServiceCollector;
import de.mnl.osgi.lf4osgi.Logger;
import de.mnl.osgi.lf4osgi.LoggerFactory;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdrupes.json.JsonBeanEncoder;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Detached;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.Session;
import org.jgrapes.portal.base.PortalSession;
import org.jgrapes.portal.base.PortalUtils;
import org.jgrapes.portal.base.Portlet.RenderMode;
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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/**
 * A portlet for displaying the OSGi log.
 */
public class LogViewerPortlet extends FreeMarkerPortlet {

    private static final Set<RenderMode> MODES = RenderMode.asSet(View);
    private ServiceCollector<LogReaderService,
            LogReaderService> logReaderCollector;
    private LogReaderService logReaderResolved;
    private LogListener logReaderListener;

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     *            on by default and that {@link Manager#fire(Event, Channel...)}
     *            sends the event to
     */
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public LogViewerPortlet(Channel componentChannel, BundleContext context) {
        super(componentChannel, true);
        logReaderCollector
            = new ServiceCollector<>(context, LogReaderService.class);
        logReaderCollector.setOnBound((ref, svc) -> subscribeTo(svc))
            .setOnModfied((ref, svc) -> subscribeTo(svc))
            .setOnUnbinding((ref, svc) -> subscribeTo(svc));
        logReaderCollector.open();
    }

    private void subscribeTo(LogReaderService logReaderService) {
        if (logReaderResolved != null
            && logReaderResolved.equals(logReaderService)) {
            return;
        }
        if (logReaderListener != null) {
            logReaderResolved.removeLogListener(logReaderListener);
        }
        logReaderResolved = logReaderService;
        if (logReaderResolved != null) {
//            logListener = logReaderResolved.addLogListener(listener);
        }

    }

    @Handler(channels = Channel.class)
    public void onStop(Stop event) {
        logReaderCollector.close();
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
            .addRenderMode(RenderMode.View)
            .addScript(new ScriptResource()
                .setRequires(new String[] { "vuejs.org" })
                .setScriptUri(event.renderSupport().portletResource(
                    type(), "LogViewer-functions.ftl.js")))
            .addCss(event.renderSupport(),
                PortalUtils.uriFromPath("LogViewer-style.css")));
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
            return Optional.of((T) new LogViewerModel(portletId));
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
            PortalSession channel) throws Exception {
        LogViewerModel portletModel = new LogViewerModel(generatePortletId());
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
            Serializable retrievedState) throws Exception {
        LogViewerModel portletModel = (LogViewerModel) retrievedState;
        renderPortlet(event, channel, portletModel);
    }

    private void renderPortlet(RenderPortletRequestBase<?> event,
            PortalSession channel, LogViewerModel portletModel)
            throws TemplateNotFoundException,
            MalformedTemplateNameException, ParseException, IOException,
            InvalidSyntaxException {
        switch (event.preferredRenderMode()) {
        case View: {
            Template tpl
                = freemarkerConfig().getTemplate("LogViewer-view.ftl.html");
            channel.respond(new RenderPortletFromTemplate(event,
                LogViewerPortlet.class, portletModel.getPortletId(),
                tpl, fmModel(event, channel, portletModel))
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            Logger log = LoggerFactory.getLogger(LogViewerPortlet.class);
            log.error(() -> "Test", new UnsupportedOperationException());
            sendAllEntries(channel, portletModel.getPortletId());
            break;
        }
        default:
            break;
        }
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

//    /**
//     * Handles a {@link BundleChanged} event by updating the information in the
//     * portal sessions.
//     *
//     * @param event the event
//     */
//    @Handler
//    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
//    public void onBundleChanged(BundleChanged event,
//            PortalSession portalSession) {
//        for (String portletId : portletIds(portalSession)) {
//            portalSession.respond(new NotifyPortletView(type(), portletId,
//                "bundleUpdates",
//                (Object) new Object[] { createBundleInfo(
//                    event.bundleEvent().getBundle(), portalSession.locale()) },
//                "*", false));
//        }
//    }

    private void sendAllEntries(PortalSession channel, String portletId) {
        final LogReaderService logReader = logReaderResolved;
        if (logReader == null) {
            return;
        }
//        channel.respond(new NotifyPortletView(type(),
//            portletId, "entries", Collections.list(logReader.getLog()).stream()
//                .map(entry -> logEntryAsMap(entry)).toArray(),
//            true));
        try {
            List<Map<?, ?>> entries = Collections.list(
                logReader.getLog()).stream() // .limit(50)
                .map(entry -> logEntryAsMap(entry))
                .collect(Collectors.toList());
//            List<Map<?, ?>> entries = new ArrayList<Map<?, ?>>();
//            for (int i = 0; i < 100; i++) {
//                entries.add(new HashMap<>());
//            }

            channel.respond(new NotifyPortletView(type(),
                portletId, "entries", entries,
                true));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private Map<String, Object> logEntryAsMap(LogEntry entry) {
        Map<String, Object> result = new HashMap<>();
        result.put("bundle", entry.getBundle().getSymbolicName());
//        result.put("exception", Optional.ofNullable(entry.getException())
//            .map(exc -> exc.getMessage()).orElse(""));
//        result.put("stacktrace", Optional.ofNullable(entry.getException())
//            .map(exc -> {
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                PrintWriter pw = new PrintWriter(out);
//                exc.printStackTrace(pw);
//                pw.close();
//                return out.toString();
//            }).orElse(""));
//        result.put("location", Optional.ofNullable(entry.getLocation())
//            .map(loc -> loc.toString()).orElse(""));
        result.put("loggerName", entry.getLoggerName());
        result.put("logLevel", entry.getLogLevel().toString());
        result.put("message", entry.getMessage());
        result.put("sequence", entry.getSequence());
//        result.put("service", Optional.ofNullable(entry.getServiceReference())
//            .map(Object::toString).orElse(""));
//        result.put("threadInfo", entry.getThreadInfo());
        result.put("time", entry.getTime());
        return result;
    }

    /**
     * The portlet's model.
     */
    @SuppressWarnings("serial")
    public class LogViewerModel extends PortletBaseModel {

        /**
         * Instantiates a new service list model.
         *
         * @param portletId the portlet id
         */
        public LogViewerModel(String portletId) {
            super(portletId);
        }

    }
}
