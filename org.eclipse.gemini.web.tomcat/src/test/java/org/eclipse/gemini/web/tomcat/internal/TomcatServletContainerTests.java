/*******************************************************************************
 * Copyright (c) 2012, 2017 SAP SE
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
 *   Violeta Georgieva - initial contribution
 *******************************************************************************/

package org.eclipse.gemini.web.tomcat.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.core.StandardContext;
import org.eclipse.gemini.web.core.spi.ContextPathExistsException;
import org.eclipse.gemini.web.core.spi.ServletContainerException;
import org.eclipse.gemini.web.core.spi.WebApplicationHandle;
import org.eclipse.gemini.web.tomcat.internal.loader.BundleWebappLoader;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.virgo.test.stubs.framework.StubFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;

public class TomcatServletContainerTests {

    private static final String FILTER_2 = "(objectClass=org.eclipse.gemini.web.tomcat.spi.ClassLoaderCustomizer)";

    private static final String FILTER_1 = "(objectClass=org.eclipse.gemini.web.tomcat.spi.JarScannerCustomizer)";

    private static final String ENGINE_NAME = "Engine";

    private static final String CONTEXT_PATH = "ContextPath";

    private BundleContext bundleContext;

    private Bundle bundle;

    private Filter filter;

    private Server server;

    private Service service;

    private Engine engine;

    private Host host;

    private ServletContext servletContext;

    private WebApplicationHandle webApplicationHandle;

    private ServiceTracker<URLConverter, URLConverter> converter;

    @Before
    public void setUp() throws Exception {
        this.bundleContext = createMock(BundleContext.class);
        this.bundle = createMock(Bundle.class);
        this.filter = createMock(Filter.class);
        this.server = createMock(Server.class);
        this.service = createMock(Service.class);
        this.engine = createMock(Engine.class);
        this.host = createMock(Host.class);
        this.servletContext = createMock(ServletContext.class);
        this.webApplicationHandle = createMock(WebApplicationHandle.class);
        this.converter = new ServiceTracker<>(this.bundleContext, createMock(StubFilter.class), null);

        expect(this.bundleContext.createFilter(FILTER_1)).andReturn(this.filter);
        expect(this.bundleContext.createFilter(FILTER_2)).andReturn(this.filter);
        expect(this.bundleContext.getProperty(OsgiAwareEmbeddedTomcat.USE_NAMING)).andReturn(OsgiAwareEmbeddedTomcat.NAMING_DISABLED);
        expect(this.bundleContext.getProperty(BundleDependenciesJarScanFilter.SCANNER_SKIP_BUNDLES_PROPERTY_NAME)).andReturn(null);

        expect(this.server.findServices()).andReturn(new Service[] { this.service }).anyTimes();
        this.server.init();
        expectLastCall();

        expect(this.service.getContainer()).andReturn(this.engine).anyTimes();

        expect(this.engine.getName()).andReturn(ENGINE_NAME);
    }

    @After
    public void tearDown() throws Exception {
        verify(this.bundleContext, this.bundle, this.filter, this.server, this.service, this.engine, this.host, this.servletContext,
            this.webApplicationHandle);
    }

    @Test
    public void testStartWebApplication() throws Exception {
        final ExtendedStandardContext standardContext = new ExtendedStandardContext();

        startExpectations(standardContext);

        final TomcatServletContainer tomcatServletContainer = createTomcatServletContainer();
        final WebApplicationHandle webApplicationHandle = new TomcatServletContainer.TomcatWebApplicationHandle(this.servletContext, standardContext,
            new BundleWebappLoader(this.bundle, null));

        try {
            tomcatServletContainer.startWebApplication(webApplicationHandle);
        } catch (RuntimeException e) {
            fail("Exception should not be thrown. " + e);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testStartWebApplicationHostNotFound() throws Exception {
        expect(this.servletContext.getContextPath()).andReturn(CONTEXT_PATH);
        expect(this.engine.findChildren()).andReturn(new Container[] {});
        expect(this.webApplicationHandle.getServletContext()).andReturn(this.servletContext);

        startWebApplication(this.webApplicationHandle);
    }

    @Test
    public void testStartWebApplicationContextAlreadyExists() throws Exception {
        expect(this.servletContext.getContextPath()).andReturn(CONTEXT_PATH);
        expect(this.engine.findChildren()).andReturn(new Container[] { this.host });
        expect(this.webApplicationHandle.getServletContext()).andReturn(this.servletContext);
        expect(this.host.findChild(CONTEXT_PATH)).andReturn(new StandardContext());

        final TomcatServletContainer tomcatServletContainer = createTomcatServletContainer();

        try {
            tomcatServletContainer.startWebApplication(this.webApplicationHandle);
        } catch (RuntimeException e) {
            assertTrue(e instanceof ContextPathExistsException);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testStartWebApplicationUnknownWebApplicationHandle() throws Exception {
        expect(this.servletContext.getContextPath()).andReturn(CONTEXT_PATH);
        expect(this.engine.findChildren()).andReturn(new Container[] { this.host });
        expect(this.webApplicationHandle.getServletContext()).andReturn(this.servletContext);
        expect(this.host.findChild(CONTEXT_PATH)).andReturn(null);

        startWebApplication(this.webApplicationHandle);
    }

    @Test(expected = ServletContainerException.class)
    public void testStartWebApplicationCannotAddToHost() throws Exception {
        final ExtendedStandardContext standardContext = new ExtendedStandardContext();

        expect(this.servletContext.getContextPath()).andReturn(CONTEXT_PATH);
        expect(this.engine.findChildren()).andReturn(new Container[] { this.host });
        expect(this.host.findChild(CONTEXT_PATH)).andReturn(null);
        expect(this.bundle.getLastModified()).andReturn(0L);
        this.host.addChild(standardContext);
        expectLastCall().andThrow(new IllegalStateException());
        this.host.removeChild(standardContext);
        expectLastCall();

        final TomcatServletContainer tomcatServletContainer = createTomcatServletContainer();
        final WebApplicationHandle webApplicationHandle = new TomcatServletContainer.TomcatWebApplicationHandle(this.servletContext, standardContext,
            new BundleWebappLoader(this.bundle, null));

        tomcatServletContainer.startWebApplication(webApplicationHandle);
    }

    @Test(expected = ServletContainerException.class)
    public void testStartWebApplicationContextIsNotAvailable() throws Exception {
        final ExtendedStandardContext standardContext = new ExtendedStandardContext();
        standardContext.setState(LifecycleState.NEW);

        startExpectations(standardContext);
        this.host.removeChild(standardContext);
        expectLastCall();

        final TomcatServletContainer tomcatServletContainer = createTomcatServletContainer();
        final WebApplicationHandle webApplicationHandle = new TomcatServletContainer.TomcatWebApplicationHandle(this.servletContext, standardContext,
            new BundleWebappLoader(this.bundle, null));

        tomcatServletContainer.startWebApplication(webApplicationHandle);
    }

    private TomcatServletContainer createTomcatServletContainer() {
        replay(this.bundleContext, this.bundle, this.filter, this.server, this.service, this.engine, this.host, this.servletContext,
            this.webApplicationHandle);
        OsgiAwareEmbeddedTomcat osgiAwareEmbeddedTomcat = new OsgiAwareEmbeddedTomcat(this.bundleContext, this.converter);
        osgiAwareEmbeddedTomcat.setServer(this.server);
        return new TomcatServletContainer(osgiAwareEmbeddedTomcat, this.bundleContext);
    }

    private void startWebApplication(WebApplicationHandle webApplicationHandle) {
        final TomcatServletContainer tomcatServletContainer = createTomcatServletContainer();

        tomcatServletContainer.startWebApplication(webApplicationHandle);
    }

    private static class ExtendedStandardContext extends StandardContext {

        private LifecycleState lifecycleState = LifecycleState.STARTED;

        @Override
        public LifecycleState getState() {
            return this.lifecycleState;
        }

        @Override
        public synchronized void setState(LifecycleState lifecycleState) {
            this.lifecycleState = lifecycleState;
        }
    }

    private void startExpectations(final ExtendedStandardContext standardContext) {
        expect(this.servletContext.getContextPath()).andReturn(CONTEXT_PATH);
        expect(this.engine.findChildren()).andReturn(new Container[] { this.host });
        expect(this.host.findChild(CONTEXT_PATH)).andReturn(null);
        expect(this.bundle.getLastModified()).andReturn(0L);
        this.host.addChild(standardContext);
        expectLastCall();
    }
}
