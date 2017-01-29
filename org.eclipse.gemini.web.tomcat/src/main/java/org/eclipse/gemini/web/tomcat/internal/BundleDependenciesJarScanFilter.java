/*******************************************************************************
 * Copyright (c) 2015 SAP SE
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.osgi.framework.BundleContext;

final class BundleDependenciesJarScanFilter implements JarScanFilter {

    private static final String COMMA_SEPARATOR = ",";

    /**
     * By default the Bundle Dependencies Jar Scanner will exclude the bundles listed below from the scanning process as
     * they do not provide TLDs and web-fragment.xml files: org.eclipse.osgi, javax.servlet, javax.servlet.jsp,
     * javax.el,javax.websocket. The default behavior can be changed with property
     * <code>org.eclipse.gemini.web.tomcat.scanner.skip.bundles</code>. The syntax is
     * <code>org.eclipse.gemini.web.tomcat.scanner.skip.bundles=&lt;bundle-symbolic-name&gt;,&lt;bundle-symbolic-name&gt;,...</code>
     */
    static final String SCANNER_SKIP_BUNDLES_PROPERTY_NAME = "org.eclipse.gemini.web.tomcat.scanner.skip.bundles";

    private static final String SCANNER_SKIP_BUNDLES_PROPERTY_VALUE_DEFAULT =
            "org.eclipse.osgi,javax.servlet,javax.servlet.jsp,javax.el,javax.websocket,javax.security.auth.message";

    private final Set<String> skipBundles;

    BundleDependenciesJarScanFilter(BundleContext bundleContext) {
        this.skipBundles = Collections.unmodifiableSet(getBundlesToSkip(bundleContext));
    }

    @Override
    public boolean check(JarScanType jarScanType, String bundleSymbolicName) {
        if (this.skipBundles.contains(bundleSymbolicName)) {
            return false;
        }
        return true;
    }

    private Set<String> getBundlesToSkip(BundleContext bundleContext) {
        Set<String> result = new HashSet<>();
        String property = bundleContext.getProperty(SCANNER_SKIP_BUNDLES_PROPERTY_NAME);

        if (property == null) {
            property = SCANNER_SKIP_BUNDLES_PROPERTY_VALUE_DEFAULT;
        }

        String[] bundlesNames = property.split(COMMA_SEPARATOR);
        for (int i = 0; bundlesNames != null && i < bundlesNames.length; i++) {
            result.add(bundlesNames[i]);
        }

        return result;
    }
}
