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


package org.jgrapes.osgi.portlets.services;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.jgrapes.portal.PortalSession;
import org.jgrapes.portal.Portlet.RenderMode;

import static org.jgrapes.portal.Portlet.RenderMode.DeleteablePreview;
import static org.jgrapes.portal.Portlet.RenderMode.View;

import org.jgrapes.portal.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.NotifyPortletView;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RenderPortlet;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.portal.freemarker.FreeMarkerPortlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

/**
 * A portlet for inspecting the services in an OSGi runtime.
 */
public class ServiceListPortlet extends FreeMarkerPortlet
	implements ServiceListener {

	private ServiceComponentRuntime scr;
	private static final Set<RenderMode> MODES = RenderMode.asSet(
			DeleteablePreview, View);
	private BundleContext context;
	
	/**
	 * Creates a new component with its channel set to the given 
	 * channel.
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 */
	public ServiceListPortlet(Channel componentChannel, BundleContext context,
			ServiceComponentRuntime scr) {
		super(componentChannel, true);
		this.context = context;
		this.scr = scr;
		context.addServiceListener(this);
	}
	
	@Handler
	public void onPortalReady(PortalReady event, PortalSession channel) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		ResourceBundle resourceBundle = resourceBundle(channel.locale());
		// Add portlet resources to page
		channel.respond(new AddPortletType(type())
				.setDisplayName(resourceBundle.getString("portletName"))
				.addScript(new ScriptResource()
						.setRequires(new String[] {"datatables.net"})
						.setScriptUri(event.renderSupport().portletResource(
								type(), "Services-functions.ftl.js")))
				.addCss(event.renderSupport().portletResource(
						type(), "Services-style.css"))
				.setInstantiable());
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#generatePortletId()
	 */
	@Override
	protected String generatePortletId() {
		return type() + "-" + super.generatePortletId();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#modelFromSession
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected <T extends Serializable> Optional<T> stateFromSession(
			Session session, String portletId, Class<T> type) {
		if (portletId.startsWith(type() + "-")) {
			return Optional.of((T)new ServiceListModel(portletId));
		}
		return Optional.empty();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doAddPortlet
	 */
	protected String doAddPortlet(AddPortletRequest event, PortalSession channel)
			throws Exception {
		ServiceListModel portletModel = new ServiceListModel(generatePortletId());
		Template tpl = freemarkerConfig().getTemplate("Services-preview.ftl.html");
		channel.respond(new RenderPortlet(
				ServiceListPortlet.class, portletModel.getPortletId(),
				templateProcessor(tpl, fmModel(event, channel, portletModel)))
				.setRenderMode(DeleteablePreview).setSupportedModes(MODES)
				.setForeground(true));
		List<Map<String,Object>> serviceInfos = Arrays.stream(
				context.getAllServiceReferences(null, null))
				.map(s -> createServiceInfo(s, channel.locale())).collect(Collectors.toList());
		channel.respond(new NotifyPortletView(type(),
				portletModel.getPortletId(), "serviceUpdates", serviceInfos, "preview", true));
		return portletModel.getPortletId();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet
	 */
	@Override
	protected void doRenderPortlet(RenderPortletRequest event,
	        PortalSession channel, String portletId, Serializable retrievedState)
	        throws Exception {
		ServiceListModel portletModel = (ServiceListModel)retrievedState;
		switch (event.renderMode()) {
		case Preview:
		case DeleteablePreview: {
			Template tpl = freemarkerConfig().getTemplate("Services-preview.ftl.html");
			channel.respond(new RenderPortlet(
					ServiceListPortlet.class, portletId, 
					templateProcessor(tpl, fmModel(event, channel, portletModel)))
					.setRenderMode(DeleteablePreview).setSupportedModes(MODES)
					.setForeground(event.isForeground()));
			List<Map<String,Object>> serviceInfos = Arrays.stream(
					context.getAllServiceReferences(null, null))
					.map(s -> createServiceInfo(s, channel.locale())).collect(Collectors.toList());
			channel.respond(new NotifyPortletView(type(),
					portletModel.getPortletId(), "serviceUpdates", serviceInfos, "preview", true));
			break;
		}
		case View: {
			Template tpl = freemarkerConfig().getTemplate("Services-view.ftl.html");
			channel.respond(new RenderPortlet(
					ServiceListPortlet.class, portletModel.getPortletId(), 
					templateProcessor(tpl, fmModel(event, channel, portletModel)))
					.setRenderMode(View).setSupportedModes(MODES)
					.setForeground(event.isForeground()));
			List<Map<String,Object>> serviceInfos = Arrays.stream(
					context.getAllServiceReferences(null, null))
					.map(s -> createServiceInfo(s, channel.locale())).collect(Collectors.toList());
			channel.respond(new NotifyPortletView(type(),
					portletModel.getPortletId(), "serviceUpdates", serviceInfos, "view", true));
			break;
		}
		default:
			break;
		}	
	}

	private Map<String,Object> createServiceInfo(ServiceReference<?> serviceRef, Locale locale) {
		Map<String, Object> result = new HashMap<>();
		result.put("id", serviceRef.getProperty(Constants.SERVICE_ID).toString());
		String[] interfaces = (String[])serviceRef.getProperty(Constants.OBJECTCLASS);
		result.put("type", String.join(", ", interfaces));
		Long bundleId = (Long)serviceRef.getProperty(Constants.SERVICE_BUNDLEID);
		result.put("bundleId", bundleId.toString());
		Bundle bundle = context.getBundle(bundleId);
		result.put("bundleName", Optional.ofNullable(bundle.getHeaders(locale.toString())
				.get("Bundle-Name")).orElse(bundle.getSymbolicName()));
		String scope = "";
		switch((String)serviceRef.getProperty(Constants.SERVICE_SCOPE)) {
		case Constants.SCOPE_BUNDLE:
			scope = "serviceScopeBundle";
			break;
		case Constants.SCOPE_PROTOTYPE:
			scope = "serviceScopePrototype";
			break;
		case Constants.SCOPE_SINGLETON:
			scope = "serviceScopeSingleton";
			break;
		}
		result.put("scope", scope);
		Integer ranking = (Integer)serviceRef.getProperty(Constants.SERVICE_RANKING);
		result.put("ranking", ranking == null ? "" : ranking.toString());
		String componentName = (String)serviceRef.getProperty("component.name");
		if (componentName != null) {
			ComponentDescriptionDTO dto = scr.getComponentDescriptionDTO(bundle, componentName);
			result.put("dsScope", "serviceScope" 
					+ dto.scope.substring(0, 1).toUpperCase() + dto.scope.substring(1));
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
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet
	 */
	@Override
	protected void doDeletePortlet(DeletePortletRequest event, PortalSession channel, 
			String portletId, Serializable portletState) throws Exception {
		channel.respond(new DeletePortlet(portletId));
	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		for (Map.Entry<PortalSession,Set<String>> e:
			portletIdsByPortalSession().entrySet()) {
			PortalSession portalSession = e.getKey();
			Map<String,Object> info = createServiceInfo(
					event.getServiceReference(), portalSession.locale());
			if (event.getType() == ServiceEvent.UNREGISTERING) {
				info.put("updateType", "unregistering");
			}
			for (String portletId: e.getValue()) {
				portalSession.respond(new NotifyPortletView(
						type(), portletId, "serviceUpdates", 
						(Object)new Object[] { info }, "*", false));
			}
		}
	}

	@SuppressWarnings("serial")
	public class ServiceListModel extends PortletBaseModel {

		public ServiceListModel(String portletId) {
			super(portletId);
		}

	}
}
