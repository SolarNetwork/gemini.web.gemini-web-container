/*******************************************************************************
 * Copyright (c) 2009, 2017 VMware Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at
 *   http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.gemini.web.tomcat.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.gemini.web.core.WebContainerProperties;
import org.eclipse.gemini.web.core.spi.ServletContainer;
import org.eclipse.gemini.web.tomcat.internal.bundleresources.BundleURLStreamHandlerService;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.virgo.util.osgi.ServiceRegistrationTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private static final String WAR_PROTOCOL = "war";

    private static final String FILTER = "(&(objectClass=" + URLConverter.class.getName()
            + ")(protocol=bundleentry))";

    private static final String EXPRESSION_FACTORY = "javax.el.ExpressionFactory";

    private static final String EXPRESSION_FACTORY_IMPL = "org.apache.el.ExpressionFactoryImpl";

    private final Object monitor = new Object();

    private final ServiceRegistrationTracker tracker = new ServiceRegistrationTracker();

    private TomcatServletContainer container;

    private ServiceTracker<URLConverter, URLConverter> urlConverterTracker;

    private String oldExpressionFactory;

    @Override
    public void start(BundleContext context) throws Exception {
        this.oldExpressionFactory = System.setProperty(EXPRESSION_FACTORY, EXPRESSION_FACTORY_IMPL);

        registerURLStreamHandler(context);
        registerConnectorDescriptors(context);

        Filter filter = context.createFilter(FILTER);
        this.urlConverterTracker = new ServiceTracker<>(context, filter, null);
        this.urlConverterTracker.open();

        TomcatServletContainer container = createContainer(context);
        container.start();

        ServiceRegistration<ServletContainer> sr = context.registerService(ServletContainer.class, container, null);
        this.tracker.track(sr);

        synchronized (this.monitor) {
            this.container = container;
        }
    }

    private void registerConnectorDescriptors(BundleContext context) {
        TomcatWebContainerProperties tomcatWebContainerProperties = new TomcatWebContainerProperties();
        ServiceRegistration<WebContainerProperties> registration = context.registerService(WebContainerProperties.class,
            tomcatWebContainerProperties, null);
        this.tracker.track(registration);
    }

    private void registerURLStreamHandler(BundleContext context) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { WAR_PROTOCOL });

        BundleURLStreamHandlerService handler = new BundleURLStreamHandlerService();
        ServiceRegistration<URLStreamHandlerService> reg = context.registerService(URLStreamHandlerService.class, handler, properties);
        this.tracker.track(reg);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        this.tracker.unregisterAll();

        TomcatServletContainer container;
        synchronized (this.monitor) {
            container = this.container;
            this.container = null;
        }
        if (container != null) {
            container.stop();
        }

        if (this.oldExpressionFactory != null) {
            System.setProperty(EXPRESSION_FACTORY, this.oldExpressionFactory);
        }

        this.urlConverterTracker.close();
    }

    private TomcatServletContainer createContainer(BundleContext context) throws BundleException {
        TomcatServletContainerFactory factory = new TomcatServletContainerFactory();
        InputStream configFile = null;
        try {
            configFile = resolveConfigFile(context);
            return factory.createContainer(configFile, context, this.urlConverterTracker);
        } finally {
            if (configFile != null) {
                try {
                    configFile.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private InputStream resolveConfigFile(BundleContext context) throws BundleException {
        return TomcatConfigLocator.resolveConfigFile(context);
    }
}
