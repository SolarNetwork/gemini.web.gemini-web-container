/*******************************************************************************
 * Copyright (c) 2012, 2017 SAP AG
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

package org.eclipse.gemini.web.extender;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.eclipse.gemini.web.core.WebApplication;
import org.eclipse.gemini.web.core.WebContainer;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class ExtenderActivatorTests {

    private static final String CLASS_NAME = "org.eclipse.gemini.web.core.WebContainer";

    private static final String FILTER_STRING = "(objectClass=org.eclipse.gemini.web.core.WebContainer)";

    private Filter filter;

    private WebContainer webContainer;

    private BundleContext bundleContext;

    @Before
    public void setUp() throws Exception {
        this.filter = createMock(Filter.class);
        this.webContainer = createMock(WebContainer.class);
        this.bundleContext = createMock(BundleContext.class);
        expect(this.bundleContext.createFilter(FILTER_STRING)).andReturn(this.filter);
        this.bundleContext.addServiceListener(isA(ServiceListener.class), isA(String.class));
        expectLastCall();
        this.bundleContext.removeServiceListener(isA(ServiceListener.class));
        expectLastCall();
    }

    @Test
    public void testStartStopNoService() throws Exception {
        expect(this.bundleContext.getServiceReferences(CLASS_NAME, null)).andReturn(null);

        replay(this.filter, this.webContainer, this.bundleContext);

        ExtenderActivator extenderActivator = new ExtenderActivator();
        extenderActivator.start(this.bundleContext);
        extenderActivator.stop(this.bundleContext);

        verify(this.filter, this.webContainer, this.bundleContext);
    }

    @Test
    public void testStartStopServiceExistsNoBundles() throws Exception {
        ServiceReference<?> serviceReference = createMock(ServiceReference.class);
        Bundle bundle = createMock(Bundle.class);
        Bundle systemBundle = createMock(Bundle.class);
        BundleContext systemBundleContext = createMock(BundleContext.class);
        expect(this.bundleContext.getBundle(0)).andReturn(systemBundle);
        expect(systemBundle.getBundleContext()).andReturn(systemBundleContext);
        expect(this.bundleContext.getServiceReferences(CLASS_NAME, null)).andReturn(new ServiceReference<?>[] { serviceReference });
        expect((WebContainer) this.bundleContext.getService(serviceReference)).andReturn(this.webContainer);
        expect(this.bundleContext.getBundle()).andReturn(bundle);
        systemBundleContext.addBundleListener(isA(BundleListener.class));
        expectLastCall();
        expect(systemBundleContext.getBundles()).andReturn(null);
        expect(serviceReference.getBundle()).andReturn(bundle);
        expect(bundle.getSymbolicName()).andReturn("");
        systemBundleContext.removeBundleListener(isA(BundleListener.class));
        expectLastCall();

        replay(this.filter, this.webContainer, this.bundleContext, serviceReference, bundle, systemBundle, systemBundleContext);

        ExtenderActivator extenderActivator = new ExtenderActivator();
        extenderActivator.start(this.bundleContext);
        extenderActivator.stop(this.bundleContext);

        verify(this.filter, this.webContainer, this.bundleContext, serviceReference, bundle, systemBundle, systemBundleContext);
    }

    @Test
    public void testStartStopServiceExistsBundlesExist() throws Exception {
        ServiceReference<?> serviceReference = createMock(ServiceReference.class);
        Bundle bundle = createMock(Bundle.class);
        Bundle bundle1 = createMock(Bundle.class);
        Bundle bundle2 = createMock(Bundle.class);
        Bundle bundle3 = createMock(Bundle.class);
        Bundle systemBundle = createMock(Bundle.class);
        BundleContext systemBundleContext = createMock(BundleContext.class);
        expect(this.bundleContext.getBundle(0)).andReturn(systemBundle);
        expect(systemBundle.getBundleContext()).andReturn(systemBundleContext);
        WebApplication webApplication = createMock(WebApplication.class);
        expect(this.bundleContext.getServiceReferences(CLASS_NAME, null)).andReturn(new ServiceReference<?>[] { serviceReference });
        expect((WebContainer) this.bundleContext.getService(serviceReference)).andReturn(this.webContainer);
        expect(this.bundleContext.getBundle()).andReturn(bundle);
        systemBundleContext.addBundleListener(isA(BundleListener.class));
        expectLastCall();
        expect(systemBundleContext.getBundles()).andReturn(new Bundle[] { bundle1, bundle2, bundle3 });
        expect(serviceReference.getBundle()).andReturn(bundle);
        expect(bundle.getSymbolicName()).andReturn("");
        expect(bundle1.getState()).andReturn(Bundle.ACTIVE);
        expect(bundle2.getState()).andReturn(Bundle.RESOLVED);
        expect(bundle3.getState()).andReturn(Bundle.ACTIVE);
        expect(this.webContainer.isWebBundle(bundle1)).andReturn(false);
        expect(this.webContainer.isWebBundle(bundle3)).andReturn(true).times(2);
        expect(this.webContainer.createWebApplication(bundle3, bundle)).andReturn(webApplication);
        webApplication.start();
        expectLastCall();
        systemBundleContext.removeBundleListener(isA(BundleListener.class));
        expectLastCall();
        webApplication.stop();
        expectLastCall();

        replay(this.filter, this.webContainer, this.bundleContext, serviceReference, bundle,
                bundle1, bundle2, bundle3, systemBundle, systemBundleContext, webApplication);

        ExtenderActivator extenderActivator = new ExtenderActivator();
        extenderActivator.start(this.bundleContext);
        extenderActivator.stop(this.bundleContext);

        verify(this.filter, this.webContainer, this.bundleContext, serviceReference, bundle,
                bundle1, bundle2, bundle3, systemBundle, systemBundleContext, webApplication);
    }

}
