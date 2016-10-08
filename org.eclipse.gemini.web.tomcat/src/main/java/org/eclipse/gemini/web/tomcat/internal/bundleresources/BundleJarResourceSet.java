/*******************************************************************************
 * Copyright (c) 2015, 2016 SAP SE
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

package org.eclipse.gemini.web.tomcat.internal.bundleresources;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.JarResourceSet;

final class BundleJarResourceSet extends JarResourceSet {

    BundleJarResourceSet(WebResourceRoot root, String webAppMount, String base, String internalPath) throws IllegalArgumentException {
        super(root, webAppMount, base, internalPath);
    }

    @Override
    protected WebResource createArchiveResource(JarEntry jarEntry, String webAppPath, Manifest manifest) {
        return new BundleJarResource(this, webAppPath, getBaseUrlString(), jarEntry);
    }

    @Override
    protected void initInternal() throws LifecycleException {
        URLConnection conn = null;
        URL baseUrl = null;
        try {
            baseUrl = new URL(getBase());
            conn = baseUrl.openConnection();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        try (JarInputStream jarIs = new JarInputStream(conn.getInputStream())) {
            setManifest(jarIs.getManifest());
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }

        setBaseUrl(baseUrl);
    }

    @Override
    protected HashMap<String,JarEntry> getArchiveEntries(boolean single) {
        synchronized (archiveLock) {
            if (archiveEntries == null && !single) {
                URLConnection conn = null;
                URL baseUrl = null;
                try {
                    baseUrl = new URL(getBase());
                    conn = baseUrl.openConnection();
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }

                archiveEntries = new HashMap<>();
                try (JarInputStream jarIs = new JarInputStream(conn.getInputStream())) {
                    JarEntry entry = jarIs.getNextJarEntry();
                    while (entry != null) {
                        archiveEntries.put(entry.getName(), entry);
                        entry = jarIs.getNextJarEntry();
                    }
                } catch (IOException ioe) {
                    // Should never happen
                    archiveEntries = null;
                    throw new IllegalStateException(ioe);
                }
            }
            return archiveEntries;
        }
    }

    @Override
    protected JarEntry getArchiveEntry(String pathInArchive) {
        URLConnection conn = null;
        URL baseUrl = null;
        try {
            baseUrl = new URL(getBase());
            conn = baseUrl.openConnection();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        JarEntry entry = null;
        try (JarInputStream jarIs = new JarInputStream(conn.getInputStream())) {
            entry = jarIs.getNextJarEntry();
            while (entry != null && !entry.getName().equals(pathInArchive)) {
                entry = jarIs.getNextJarEntry();
            }

            return entry;
        } catch (IOException ioe) {
            // Should never happen
            throw new IllegalStateException(ioe);
        }
    }

}
