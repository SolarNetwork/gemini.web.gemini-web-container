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

import java.io.InputStream;

import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

final class TomcatServletContainerFactory {

    public TomcatServletContainer createContainer(InputStream configuration, BundleContext context,
            ServiceTracker<URLConverter, URLConverter> urlConverterTracker) {
        OsgiAwareEmbeddedTomcat catalina = new OsgiAwareEmbeddedTomcat(context, urlConverterTracker);
        catalina.configure(configuration);

        return new TomcatServletContainer(catalina, context);
    }
}
