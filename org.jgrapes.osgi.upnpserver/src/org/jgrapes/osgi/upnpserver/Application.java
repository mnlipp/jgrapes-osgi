/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016,2018  Michael N. Lipp
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

package org.jgrapes.osgi.upnpserver;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ResourceBundle;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.InMemorySessionManager;
import org.jgrapes.http.LanguageSelector;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.PostRequest;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.SslServer;
import org.jgrapes.net.TcpServer;
import org.jgrapes.osgi.core.ComponentCollector;
import org.jgrapes.portal.KVStoreBasedPortalPolicy;
import org.jgrapes.portal.PageResourceProviderFactory;
import org.jgrapes.portal.Portal;
import org.jgrapes.portal.PortalLocalBackedKVStore;
import org.jgrapes.portal.PortletComponentFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 */
public class Application extends Component implements BundleActivator {

    private static BundleContext context;
    private Application app;

    /**
     * Returns the context passed to the application.
     *
     * @return the bundle context
     */
    public static BundleContext context() {
        return context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
     * BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        Application.context = context;
        // The demo component is the application
        app = new Application();
        // Attach a general nio dispatcher
        app.attach(new NioDispatcher());

        // Create TLS "converter"
        KeyStore serverStore = KeyStore.getInstance("JKS");
        try (InputStream storeData
            = Application.class.getResourceAsStream("/localhost.jks")) {
            serverStore.load(storeData, "nopass".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverStore, "nopass".toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
        // Create a TCP server for SSL
        Channel securedNetwork = app.attach(
            new TcpServer().setServerAddress(new InetSocketAddress(7443))
                .setBacklog(3000).setConnectionLimiter(new PermitsPool(50)));
        // Network level unencrypted channel.
        Channel httpTransport = new NamedChannel("httpTransport");
        app.attach(new SslServer(httpTransport, securedNetwork, sslContext));

        // Create an HTTP server as converter between transport and application
        // layer.
        app.attach(new HttpServer(app,
            httpTransport, GetRequest.class, PostRequest.class));

        // Build application layer
        app.attach(new InMemorySessionManager(app.channel()));
        app.attach(new LanguageSelector(app.channel()));
        app.attach(new FileStorage(app.channel(), 65536));
        Portal portal = app.attach(new Portal(Channel.SELF, app.channel(),
            new URI("/portal/")))
            .setResourceBundleSupplier(lang -> ResourceBundle.getBundle(
                getClass().getPackage().getName() + ".portal-l10n", lang,
                ResourceBundle.Control.getNoFallbackControl(
                    ResourceBundle.Control.FORMAT_DEFAULT)));
        portal.attach(new PortalLocalBackedKVStore(
            portal, portal.prefix().getPath()));
        portal.attach(new KVStoreBasedPortalPolicy(portal));
        portal.attach(new NewPortalSessionPolicy(portal));
        portal.attach(new ComponentCollector<>(
            portal, context, PageResourceProviderFactory.class));
        portal.attach(new ComponentCollector<>(
            portal, context, PortletComponentFactory.class));
        Components.start(app);

//		context().addServiceListener(new RootDeviceListener(),
//                "(&" + "("+Constants.OBJECTCLASS 
//                		+ "=" + UPnPDevice.class.getName() + ")"
//                	 + "("+UPnPDevice.UDN+"=*)" + ")");
//		
//		ServiceReference[] roots = null;
//		roots = context().getServiceReferences(
//				UPnPDevice.class.getName(),
//				"(&" + "("+Constants.OBJECTCLASS 
//						+ "=" + UPnPDevice.class.getName() + ")"
//					 + "("+UPnPDevice.UDN+"=*)" + ")");
//		roots = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        app.fire(new Stop(), Channel.BROADCAST);
        Components.awaitExhaustion();
    }

    /**
     * The main method.
     *
     * @param args command line arguments
     * @throws Exception the exception
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public static void main(String[] args) throws Exception {
        new Application().start(null);
    }

}
