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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;

import org.osgi.service.url.AbstractURLStreamHandlerService;

public class BundleURLStreamHandlerService extends AbstractURLStreamHandlerService {

    private static final String WAR_BUNDLE_ENTRY_SCHEMA = "war:bundle";

    private static final String WAR_TO_ENTRY_SEPARATOR = "\\^/";

    private static final String WAR_TO_ENTRY_SEPARATOR_NEW = "\\*/";

    @Override
    public URLConnection openConnection(URL u) throws IOException {
        return new BundleURLConnection(u);
    }

    private static class BundleURLConnection extends URLConnection {

        private final URLConnection wrappedUrlConnection;
        private boolean connected = false;

        BundleURLConnection(URL url) throws IOException {
            super(url);
            this.wrappedUrlConnection = warToBundle(url).openConnection();
        }

        @Override
        public void connect() throws IOException {
            if (!connected) {
                this.wrappedUrlConnection.connect();
                this.connected = true;
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return this.wrappedUrlConnection.getInputStream();
        }

        @Override
        public Permission getPermission() throws IOException {
            return this.wrappedUrlConnection.getPermission();
        }

        private URL warToBundle(URL u) throws MalformedURLException {
            String url = u.toExternalForm();
            if (url.startsWith(WAR_BUNDLE_ENTRY_SCHEMA)) {
                String path = url.substring(4);
                if (path.contains("*/")) {
                    path = path.replaceFirst(WAR_TO_ENTRY_SEPARATOR_NEW, "");
                } else {
                    path = path.replaceFirst(WAR_TO_ENTRY_SEPARATOR, "");
                }
                return new URL(path);
            } else {
                return u;
            }
        }

    }
}
