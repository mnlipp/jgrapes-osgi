/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

package org.jgrapes.osgi.portlets.bundles;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.PortalView;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.NotifyPortletModel;
import org.jgrapes.portal.events.NotifyPortletView;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RenderPortletFromProvider;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.portal.freemarker.FreeMarkerPortlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import static org.jgrapes.portal.Portlet.*;
import static org.jgrapes.portal.Portlet.RenderMode.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 
 */
public class BundleListPortlet extends FreeMarkerPortlet {

	private final static Set<RenderMode> MODES = RenderMode.asSet(
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
	public BundleListPortlet(Channel componentChannel, BundleContext context) {
		super(componentChannel);
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#generatePortletId()
	 */
	@Override
	protected String generatePortletId() {
		return type() + "-" + super.generatePortletId();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#createPortletModel()
	 */
	@Override
	protected PortletBaseModel createPortletModel() {
		BundleListModel model = new BundleListModel(generatePortletId());
		return model;
	}

	@Handler
	public void onPortalReady(PortalReady event, IOSubchannel channel) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		ResourceBundle resourceBundle = resourceBundle(locale(channel));
		// Add portlet resources to page
		channel.respond(new AddPortletType(type())
				.setDisplayName(resourceBundle.getString("portletName"))
				.addScript(PortalView.uriFromPath("Bundles-functions.js"))
				.addCss(PortalView.uriFromPath("Bundles-style.css"))
				.setInstantiable());
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#modelFromSession(org.jgrapes.io.IOSubchannel, java.lang.String)
	 */
	@Override
	protected Optional<PortletBaseModel> modelFromSession(
			Session session, String portletId) {
		Optional<PortletBaseModel> optModel 
			= super.modelFromSession(session, portletId);
		if (optModel.isPresent()) {
			return optModel;
		}
		if (portletId.startsWith(type() + "-")) {
			BundleListModel model = new BundleListModel(portletId);
			return Optional.of(addToSession(session, model));
		}
		return Optional.empty();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doAddPortlet(org.jgrapes.portal.events.AddPortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doAddPortlet(AddPortletRequest event, IOSubchannel channel,
	        Session session, PortletBaseModel portletModel) throws Exception {
		Template tpl = freemarkerConfig().getTemplate("Bundles-preview.ftlh");
		Map<String, Object> baseModel 
			= freemarkerBaseModel(event.renderSupport());
		channel.respond(new RenderPortletFromProvider(
				BundleListPortlet.class, portletModel.getPortletId(),
				DeleteablePreview, MODES, newContentProvider(tpl, 
						freemarkerModel(baseModel, portletModel, channel)),
				true));
	}

	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet(org.jgrapes.portal.events.DeletePortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doDeletePortlet(DeletePortletRequest event,
	        IOSubchannel channel, Session session, PortletBaseModel portletModel)
	        throws Exception {
		channel.respond(new DeletePortlet(portletModel.getPortletId()));
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet(org.jgrapes.portal.events.RenderPortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doRenderPortlet(RenderPortletRequest event,
	        IOSubchannel channel, Session session, PortletBaseModel retrievedModel)
	        throws Exception {
		Map<String, Object> baseModel 
			= freemarkerBaseModel(event.renderSupport());
		switch (event.renderMode()) {
		case Preview:
		case DeleteablePreview: {
			Template tpl = freemarkerConfig().getTemplate("Bundles-preview.ftlh");
			channel.respond(new RenderPortletFromProvider(
					BundleListPortlet.class, retrievedModel.getPortletId(), 
					DeleteablePreview, MODES,	newContentProvider(tpl, 
							freemarkerModel(baseModel, retrievedModel, channel)),
					event.isForeground()));
			break;
		}
		case View: {
			Template tpl = freemarkerConfig().getTemplate("Bundles-view.ftlh");
			channel.respond(new RenderPortletFromProvider(
					BundleListPortlet.class, retrievedModel.getPortletId(), 
					View, MODES, newContentProvider(tpl, 
							freemarkerModel(baseModel, retrievedModel, channel)),
					event.isForeground()));
			List<Map<String,Object>> bundleInfos = Arrays.stream(context.getBundles())
					.map(b -> createBundleInfo(b, locale(channel))).collect(Collectors.toList());
			channel.respond(new NotifyPortletView(type(),
					event.portletId(), "updateBundles", bundleInfos));
			break;
		}
		default:
			break;
		}	
	}
	
	private Map<String,Object> createBundleInfo(Bundle bundle, Locale locale) {
		Map<String, Object> result = new HashMap<>();
		result.put("id", bundle.getBundleId());
		result.put("name", Optional.ofNullable(bundle.getHeaders(locale.toString())
				.get("Bundle-Name")).orElse(bundle.getSymbolicName()));
		result.put("symbolicName", bundle.getSymbolicName());
		result.put("version", bundle.getVersion().toString());
		result.put("category", Optional.ofNullable(bundle.getHeaders(locale.toString())
				.get("Bundle-Category")).orElse(""));
		ResourceBundle rb = resourceBundle(locale);
		result.put("state", rb.getString("bundleState_" + bundle.getState()));
		result.put("actions", "");
		return result;
	}
	
	@Handler
	public void onChangePortletModel(NotifyPortletModel event,
			IOSubchannel channel) throws TemplateNotFoundException, 
			MalformedTemplateNameException, ParseException, IOException {
		Session session = session(channel);
		Optional<PortletBaseModel> optPortletModel 
			= modelFromSession(session, event.portletId());
		if (!optPortletModel.isPresent()) {
			return;
		}
	
		event.stop();
		Bundle bundle = context.getBundle(event.params().getInt(0));
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
			}
		} catch (BundleException e) {
			// ignore
		}
	}
	
	@SuppressWarnings("serial")
	public class BundleListModel extends PortletBaseModel {

		public BundleListModel(String portletId) {
			super(portletId);
		}

		public Bundle[] bundles() {
			return context.getBundles();
		}
	}
}
