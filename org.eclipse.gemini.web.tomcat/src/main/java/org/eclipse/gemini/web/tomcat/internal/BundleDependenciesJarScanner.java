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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.jasper.servlet.JasperInitializer;
import org.apache.tomcat.Jar;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.JarFactory;
import org.apache.tomcat.websocket.server.WsSci;
import org.eclipse.gemini.web.tomcat.internal.loader.BundleWebappClassLoader;
import org.eclipse.gemini.web.tomcat.internal.support.BundleDependencyDeterminer;
import org.eclipse.gemini.web.tomcat.internal.support.BundleFileResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>JarScanner</code> implementation that passes each of the {@link Bundle}'s dependencies to the
 * {@link JarScannerCallback}.
 *
 * <p />
 *
 * <strong>Concurrent Semantics</strong><br />
 *
 * Thread-safe.
 *
 */
final class BundleDependenciesJarScanner implements JarScanner {

    private static final String JAR_URL_SUFFIX = "!/";

    private static final String JAR_URL_PREFIX = "jar:";

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleDependenciesJarScanner.class);

    private final BundleDependencyDeterminer bundleDependencyDeterminer;

    private final BundleFileResolver bundleFileResolver;

    private JarScanFilter jarScanFilter;

    BundleDependenciesJarScanner(BundleDependencyDeterminer bundleDependencyDeterminer, BundleFileResolver bundleFileResolver,
        BundleContext bundleContext) {
        this.bundleDependencyDeterminer = bundleDependencyDeterminer;
        this.bundleFileResolver = bundleFileResolver;
        this.jarScanFilter = new BundleDependenciesJarScanFilter(bundleContext);
    }

    @Override
    public JarScanFilter getJarScanFilter() {
        return this.jarScanFilter;
    }

    @Override
    public void setJarScanFilter(JarScanFilter jarScanFilter) {
        this.jarScanFilter = jarScanFilter;
    }

    @Override
    public void scan(JarScanType jarScanType, ServletContext context, JarScannerCallback callback) {
        ClassLoader classLoader = context.getClassLoader();
        if (classLoader instanceof BundleWebappClassLoader) {
            Bundle bundle = ((BundleWebappClassLoader) classLoader).getBundle();
            scanDependentBundles(bundle, jarScanType, callback);
        }
    }

    private void scanDependentBundles(Bundle rootBundle, JarScanType jarScanType, JarScannerCallback callback) {
        Bundle apacheWebsocketBundle = FrameworkUtil.getBundle(WsSci.class);
        if (apacheWebsocketBundle != null) {
            scanBundle(apacheWebsocketBundle, callback, false);
        }

        Bundle apacheJasperBundle = FrameworkUtil.getBundle(JasperInitializer.class);
        if (apacheJasperBundle != null) {
            scanBundle(apacheJasperBundle, callback, false);
        }

        Set<Bundle> dependencies = this.bundleDependencyDeterminer.getDependencies(rootBundle);

        for (Bundle bundle : dependencies) {
            if (getJarScanFilter().check(jarScanType, bundle.getSymbolicName())) {
                scanBundle(bundle, callback, true);
            }
        }
    }

    private void scanBundle(Bundle bundle, JarScannerCallback callback, boolean isWebapp) {
        File bundleFile = this.bundleFileResolver.resolve(bundle);
        if (bundleFile != null) {
            scanBundleFile(bundleFile, callback, isWebapp);
        } else {
            scanJarUrlConnection(bundle, callback, isWebapp);
        }
    }

    private void scanJarUrlConnection(Bundle bundle, JarScannerCallback callback, boolean isWebapp) {
        URL bundleUrl;
        URL root = bundle.getEntry("/");
        try {
            bundleUrl = new URL(JAR_URL_PREFIX + root.toExternalForm() + JAR_URL_SUFFIX);
        } catch (MalformedURLException e) {
            LOGGER.warn("Failed to create jar: url for bundle location [" + root + "].");
            return;
        }

        scanBundleUrl(bundleUrl, callback, isWebapp);
    }

    private void scanBundleFile(File bundleFile, JarScannerCallback callback, boolean isWebapp) {
        if (bundleFile.isDirectory()) {
            try {
                callback.scan(bundleFile, null, isWebapp);
            } catch (IOException e) {
                LOGGER.warn("Failure when attempting to scan bundle file [" + bundleFile + "].", e);
            }
        } else {
            URL bundleUrl;
            try {
                bundleUrl = new URL(JAR_URL_PREFIX + bundleFile.toURI().toURL() + JAR_URL_SUFFIX);
            } catch (MalformedURLException e) {
                LOGGER.warn("Failed to create jar: url for bundle file [" + bundleFile + "].");
                return;
            }
            scanBundleUrl(bundleUrl, callback, isWebapp);
        }
    }

    private void scanBundleUrl(URL url, JarScannerCallback callback, boolean isWebapp) {
        if ("jar".equals(url.getProtocol()) || url.getPath().endsWith(".jar")) {
            try (Jar jar = JarFactory.newInstance(url)) {
                callback.scan(jar, null, isWebapp);
            } catch (IOException e) {
                LOGGER.warn("Failure when attempting to scan bundle via jar URL [" + url + "].", e);
            }
        }
    }
}
