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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.gemini.web.core.spi.ServletContainerException;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.virgo.test.stubs.framework.StubFilter;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class TomcatServletContainerFactoryTests {

    private BundleContext bundleContext;

    private ServiceTracker<URLConverter, URLConverter> converter;

    @Before
    public void setUp() throws Exception {
        this.bundleContext = createMock(BundleContext.class);
        this.converter = new ServiceTracker<>(this.bundleContext, createMock(StubFilter.class), null);
        expect(this.bundleContext.getProperty(TomcatConfigLocator.CONFIG_PATH_FRAMEWORK_PROPERTY)).andReturn(null);
        expect(this.bundleContext.getProperty(OsgiAwareEmbeddedTomcat.USE_NAMING)).andReturn(null);
        expect(this.bundleContext.getProperty(BundleDependenciesJarScanFilter.SCANNER_SKIP_BUNDLES_PROPERTY_NAME)).andReturn(null);
        expect(this.bundleContext.createFilter("(objectClass=org.eclipse.gemini.web.tomcat.spi.ClassLoaderCustomizer)")).andReturn(null);
        expect(this.bundleContext.createFilter("(objectClass=org.eclipse.gemini.web.tomcat.spi.JarScannerCustomizer)")).andReturn(null);
        expect(this.bundleContext.getBundle()).andReturn(null);
    }

    @Test
    public void testCreateContainerWithConfigFile() throws Exception {
        TomcatServletContainerFactory factory = new TomcatServletContainerFactory();
        replay(this.bundleContext);
        TomcatServletContainer container = factory.createContainer(
                Files.newInputStream(Paths.get("src/test/resources/server.xml")), this.bundleContext, this.converter);
        assertNotNull(container);
        verify(this.bundleContext);
    }

    @Test(expected = ServletContainerException.class)
    public void testCreateContainerWithInvalidConfigFile() throws Exception {
        TomcatServletContainerFactory factory = new TomcatServletContainerFactory();
        replay(this.bundleContext);
        TomcatServletContainer container = factory.createContainer(
                Files.newInputStream(Paths.get("src/test/resources/invalid-server.xml")), this.bundleContext, this.converter);
        assertNotNull(container);
        verify(this.bundleContext);
    }
}
