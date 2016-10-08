/*******************************************************************************
 * Copyright (c) 2012, 2016 SAP SE
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

package org.eclipse.gemini.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;

import org.apache.tomcat.Jar;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.JarFactory;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class BundleJarScanner implements JarScanner {

    private static final String JAR_URL_SUFFIX = "!/";

    private static final String JAR_URL_PREFIX = "jar:";

    private static final String REFERENCE_URL_PREFIX = "reference";

    @Override
    public void scan(JarScanType jarScanType, ServletContext servletContext, JarScannerCallback jarScannerCallback) {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle != null) {
            scanBundle(bundle, jarScannerCallback);
        }
    }

    @Override
    public JarScanFilter getJarScanFilter() {
        return null;
    }

    @Override
    public void setJarScanFilter(JarScanFilter jarScanFilter) {
        // no-op
    }

    private void scanBundle(Bundle bundle, JarScannerCallback callback) {
        File bundleFile = resolve(bundle);
        if (bundleFile != null) {
            scanBundleFile(bundleFile, callback);
        } else {
            scanJarUrlConnection(bundle, callback);
        }
    }

    private void scanJarUrlConnection(Bundle bundle, JarScannerCallback callback) {
        URL bundleUrl;
        String bundleLocation = bundle.getLocation();
        try {
            bundleUrl = new URL(bundleLocation);
            if (REFERENCE_URL_PREFIX.equals(bundleUrl.getProtocol())) {
                bundleUrl = new URL(JAR_URL_PREFIX + bundleUrl.getFile() + JAR_URL_SUFFIX);
            } else {
                bundleUrl = new URL(JAR_URL_PREFIX + bundleLocation + JAR_URL_SUFFIX);
            }
        } catch (MalformedURLException e) {
            System.out.println("Failed to create jar: url for bundle location " + bundleLocation);
            return;
        }

        scanBundleUrl(bundleUrl, callback);
    }

    private void scanBundleFile(File bundleFile, JarScannerCallback callback) {
        if (bundleFile.isDirectory()) {
            try {
                callback.scan(bundleFile, null, true);
            } catch (IOException e) {
                System.out.println("Failure when attempting to scan bundle file '" + bundleFile + "':" + e.getMessage());
            }
        } else {
            URL bundleUrl;
            try {
                bundleUrl = new URL(JAR_URL_PREFIX + bundleFile.toURI().toURL() + JAR_URL_SUFFIX);
            } catch (MalformedURLException e) {
                System.out.println("Failed to create jar: url for bundle file " + bundleFile);
                return;
            }
            scanBundleUrl(bundleUrl, callback);
        }
    }

    private void scanBundleUrl(URL url, JarScannerCallback callback) {
        if ("jar".equals(url.getProtocol()) || url.getPath().endsWith(".jar")) {
            try (Jar jar = JarFactory.newInstance(url)) {
                callback.scan(jar, null, true);
            } catch (IOException e) {
                System.out.println("Failure when attempting to scan bundle via jar URL '" + url + "':" + e.getMessage());
            }
        }
    }

    private File resolve(Bundle bundle) {
        BundleFile bundleFile = getBundleFile(bundle);
        if (bundleFile != null) {
            File file = bundleFile.getBaseFile();
            System.out.println("Resolved bundle '" + bundle.getSymbolicName() + "' to file '" + file.getAbsolutePath() + "'");
            return file;
        }
        return null;
    }

    private BundleFile getBundleFile(Bundle bundle) {
        if (bundle instanceof EquinoxBundle) {
            EquinoxBundle eb = (EquinoxBundle) bundle;
            Generation current = (Generation) eb.getModule().getCurrentRevision().getRevisionInfo();
            return current.getBundleFile();
        }
        return null;
    }

}
