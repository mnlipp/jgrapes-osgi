/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2020  Michael N. Lipp
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

package org.jgrapes.osgi.webconlet.logviewer;

import de.mnl.osgi.coreutils.ServiceCollector;
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleConnection;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConlet;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.events.SetLocale;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/**
 * A conlet for displaying the OSGi log.
 */
public class LogViewerConlet extends FreeMarkerConlet<Serializable> {

    private static final Set<RenderMode> MODES
        = RenderMode.asSet(RenderMode.View);
    private final ServiceCollector<LogReaderService,
            LogReaderService> logReaderCollector;
    private LogReaderService logReaderResolved;
    private final LogListener logReaderListener;

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     *            on by default and that {@link Manager#fire(Event, Channel...)}
     *            sends the event to
     */
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public LogViewerConlet(Channel componentChannel, BundleContext context) {
        super(componentChannel);
        logReaderListener = new LogListener() {
            @Override
            public void logged(LogEntry entry) {
                addEntry(entry);
            }
        };
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
        // Got a new log reader service.
        if (logReaderResolved != null) {
            logReaderResolved.removeLogListener(logReaderListener);
        }
        logReaderResolved = logReaderService;
        if (logReaderResolved != null) {
            logReaderResolved.addLogListener(logReaderListener);
        }

    }

    /**
     * Detach from OSGi framework.
     *
     * @param event the event
     */
    @Handler(channels = Channel.class)
    public void onStop(Stop event) {
        logReaderCollector.close();
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
        // Add conlet resources to page
        channel.respond(new AddConletType(type())
            .setDisplayNames(
                localizations(channel.supportedLocales(), "conletName"))
            .addRenderMode(RenderMode.View)
            .addScript(new ScriptResource()
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "LogViewer-functions.ftl.js"))
                .setScriptType("module"))
            .addCss(event.renderSupport(),
                WebConsoleUtils.uriFromPath("LogViewer-style.css")));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleConnection channel, String conletId,
            Serializable conletState)
            throws Exception {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("LogViewer-view.ftl.html");
            channel.respond(new RenderConlet(type(), conletId,
                processTemplate(event, tpl,
                    fmModel(event, channel, conletId, conletState)))
                        .setRenderAs(
                            RenderMode.View.addModifiers(event.renderAs()))
                        .setSupportedModes(MODES));
            sendAllEntries(channel, conletId);
            renderedAs.add(RenderMode.View);
        }
        return renderedAs;
    }

    private void sendAllEntries(ConsoleConnection channel, String conletId) {
        final LogReaderService logReader = logReaderResolved;
        if (logReader == null) {
            return;
        }
        channel.respond(new NotifyConletView(type(),
            conletId, "entries",
            (Object) Collections.list(logReader.getLog()).stream()
                .map(entry -> logEntryAsMap(entry)).toArray()));
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void addEntry(LogEntry entry) {
        for (ConsoleConnection connection : trackedConnections()) {
            for (String conletId : conletIds(connection)) {
                connection.respond(new NotifyConletView(type(),
                    conletId, "addEntry", logEntryAsMap(entry))
                        .disableTracking());
            }
        }
    }

    private Map<String, Object> logEntryAsMap(LogEntry entry) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> result = new HashMap<>();
        result.put("bundle",
            Optional
                .ofNullable(entry.getBundle().getHeaders().get("Bundle-Name"))
                .orElse(entry.getBundle().getSymbolicName()));
        result.put("exception", Optional.ofNullable(entry.getException())
            .map(exc -> exc.getMessage()).orElse(""));
        result.put("stacktrace", Optional.ofNullable(entry.getException())
            .map(exc -> {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter printWriter = new PrintWriter(out);
                exc.printStackTrace(printWriter);
                printWriter.close();
                return out.toString();
            }).orElse(""));
        result.put("location", Optional.ofNullable(entry.getLocation())
            .map(loc -> loc.toString()).orElse(""));
        result.put("loggerName", entry.getLoggerName());
        result.put("logLevel", entry.getLogLevel().toString());
        result.put("message", entry.getMessage());
        result.put("sequence", entry.getSequence());
        result.put("service", Optional.ofNullable(entry.getServiceReference())
            .map(Object::toString).orElse(""));
        result.put("threadInfo", entry.getThreadInfo());
        result.put("time", entry.getTime());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.console.AbstractConlet#doNotifyConletModel
     */
    @Override
    @SuppressWarnings({ "PMD.SwitchStmtsShouldHaveDefault",
        "PMD.TooFewBranchesForASwitchStatement" })
    protected void doUpdateConletState(NotifyConletModel event,
            ConsoleConnection channel, Serializable conletState)
            throws Exception {
        event.stop();
        switch (event.method()) {
        case "resync":
            sendAllEntries(channel, event.conletId());
            break;
        }
    }

    @Override
    protected boolean doSetLocale(SetLocale event, ConsoleConnection channel,
            String conletId) throws Exception {
        return true;
    }
}
