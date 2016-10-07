/*******************************************************************************
 * Copyright (c) 2009, 2016 VMware Inc.
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

package org.eclipse.gemini.web.tomcat.internal.loading;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.gemini.web.tomcat.internal.support.BundleFileResolver;
import org.eclipse.gemini.web.tomcat.internal.support.BundleFileResolverFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public final class BundleEntry {

    private static final String WEB_INF_DOT = "WEB-INF.";

    private static final String META_INF_DOT = "META-INF.";

    private static final String META_INF = "META-INF";

    private static final String OSGI_INF_DOT = "OSGI-INF.";

    private static final String OSGI_OPT_DOT = "OSGI-OPT.";

    private static final String PATH_SEPARATOR = "/";

    private static final String DOT = ".";

    private final String path;

    private final Bundle bundle;

    private final List<Bundle> fragments;

    private final BundleFileResolver bundleFileResolver = BundleFileResolverFactory.createBundleFileResolver();

    private final boolean checkEntryPath;

    private String bundleLocationCanonicalPath;

    private boolean isBundleLocationDirectory;

    public BundleEntry(Bundle bundle) {
        this.path = "";
        this.bundle = bundle;
        this.fragments = getFragments(bundle);
        this.checkEntryPath = checkEntryPath();
        File bundleLocation = this.bundleFileResolver.resolve(bundle);
        if (bundleLocation != null) {
            try {
                this.bundleLocationCanonicalPath = bundleLocation.getCanonicalPath();
            } catch (IOException e) {
            }
            if (bundleLocation.isDirectory()) {
                this.isBundleLocationDirectory = true;
            }
        }
    }

    private BundleEntry(Bundle bundle, List<Bundle> fragments, String path, boolean checkEntryPath, String bundleLocationCanonicalPath,
        boolean isBundleLocationDirectory) {
        this.path = path;
        this.bundle = bundle;
        this.fragments = fragments;
        this.checkEntryPath = checkEntryPath;
        this.bundleLocationCanonicalPath = bundleLocationCanonicalPath;
        this.isBundleLocationDirectory = isBundleLocationDirectory;
    }

    public Bundle getBundle() {
        return this.bundle;
    }

    public List<BundleEntry> list() {
        List<BundleEntry> entries = new ArrayList<BundleEntry>();
        Set<String> paths = getEntryPathsFromBundle();
        if (paths != null) {
            Iterator<String> iterator = paths.iterator();
            while (iterator.hasNext()) {
                String subPath = iterator.next();
                entries.add(createBundleEntry(subPath));
            }
        }
        return entries;
    }

    private BundleEntry createBundleEntry(String path) {
        return new BundleEntry(this.bundle, this.fragments, path, this.checkEntryPath, this.bundleLocationCanonicalPath,
            this.isBundleLocationDirectory);
    }

    private Set<String> getEntryPathsFromBundle() {
        Set<String> paths = getEntryPathsFromBundle(this.bundle);

        for (int i = 0; i < this.fragments.size(); i++) {
            paths.addAll(getEntryPathsFromBundle(this.fragments.get(i)));
        }

        if (paths.isEmpty()) {
            return null;
        }

        return paths;
    }

    private Set<String> getEntryPathsFromBundle(Bundle bundle) {
        final Enumeration<String> ep = bundle.getEntryPaths(this.path);

        Set<String> paths = new HashSet<String>();
        if (ep != null) {
            while (ep.hasMoreElements()) {
                paths.add(ep.nextElement());
            }
        }

        return paths;
    }

    public Entry<BundleEntry, URL> getEntry(String subPath) {
        String finalPath = this.path + subPath;
        URL entryURL = getEntryFromBundle(finalPath);
        if (entryURL != null) {
            Map<BundleEntry, URL> result = new HashMap<BundleEntry, URL>();
            result.put(createBundleEntry(finalPath), entryURL);
            return result.entrySet().iterator().next();
        } else {
            return null;
        }
    }

    /**
     * This method has been generalized from this.bundle.getEntry(path) to allow entries to be supplied by a fragment.
     */
    private URL getEntryFromBundle(String path) {
        if (this.checkEntryPath
            && (checkNotAttemptingToAccess(path, META_INF_DOT) || checkNotAttemptingToAccess(path, WEB_INF_DOT)
                || checkNotAttemptingToAccess(path, OSGI_INF_DOT) || checkNotAttemptingToAccess(path, OSGI_OPT_DOT))) {
            return null;
        }

        if (path.endsWith(PATH_SEPARATOR) || path.length() == 0) {
            URL url = this.bundle.getEntry(path);
            if (url == null) {
                // try fragments
                for (int i = 0; i < this.fragments.size(); i++) {
                    Bundle b = this.fragments.get(i);
                    url = b.getEntry(path);
                    if (url != null) {
                        break;
                    }
                }
            }
            return url;
        }

        String searchPath;
        String searchFile;
        int lastSlashIndex = path.lastIndexOf(PATH_SEPARATOR);
        if (lastSlashIndex == -1) {
            searchPath = PATH_SEPARATOR;
            searchFile = path;
        } else {
            searchPath = path.substring(0, lastSlashIndex);
            searchFile = path.substring(lastSlashIndex + 1);
        }

        if (searchFile.equals(DOT)) {
            return this.bundle.getEntry(path.substring(0, path.length() - 1));
        }

        Enumeration<URL> entries = this.bundle.findEntries(searchPath, searchFile, false);

        if (entries != null) {
            if (entries.hasMoreElements()) {
                return entries.nextElement();
            }
        }

        return null;
    }

    private boolean checkNotAttemptingToAccess(String path, String prefix) {
        return path.startsWith(prefix + PATH_SEPARATOR) || path.startsWith(PATH_SEPARATOR + prefix + PATH_SEPARATOR)
            || path.startsWith(DOT + PATH_SEPARATOR + prefix + PATH_SEPARATOR);
    }

    public String getName() {
        String name = this.path;

        if (name.endsWith(PATH_SEPARATOR)) {
            name = name.substring(0, this.path.length() - 1);
        }

        int index = name.lastIndexOf(PATH_SEPARATOR);
        if (index > -1) {
            name = name.substring(index + 1);
        }

        if (name.length() == 0) {
            return PATH_SEPARATOR;
        } else {
            return name;
        }
    }

    public URL getURL() {
        return getEntryFromBundle(this.path);
    }

    public String getPath() {
        return this.path;
    }

    public static boolean isDirectory(URL url) {
        return url.getFile().endsWith(PATH_SEPARATOR);
    }

    @Override
    public String toString() {
        return String.format("BundleEntry [bundle=%s,path=%s]", this.bundle, this.path);
    }

    /**
     * Returns the bundle entry size. If the BundleFileResolver is EquinoxBundleFileResolver then we will use equinox
     * specific functionality to get BundleEntry and its size. If the BundleFileResolver is NoOpBundleFileResolver we
     * will use URLConnection.getContentLength(). Note: URLConnection.getContentLength() returns "int", if the bundle
     * entry size exceeds max "int", then the content length will not be correct.
     * 
     * @return the bundle entry size
     */
    public long getContentLength(URLConnection urlConnection) {
        long size = this.bundleFileResolver.resolveBundleEntrySize(this.bundle, this.path);
        if (size == -1 && urlConnection != null) {
            size = urlConnection.getContentLength();
        }
        return size;
    }

    private List<Bundle> getFragments(Bundle bundle) {
        List<Bundle> fragments = new ArrayList<Bundle>();
        BundleRevision bundleRevision = bundle.adapt(BundleRevision.class);
        if (bundleRevision != null) {
            BundleWiring bundleWiring = bundleRevision.getWiring();
            List<BundleWire> bundleWires = bundleWiring.getProvidedWires(BundleRevision.HOST_NAMESPACE);
            for (int i = 0; bundleWires != null && i < bundleWires.size(); i++) {
                fragments.add(bundleWires.get(i).getRequirerWiring().getRevision().getBundle());
            }
        }
        return fragments;
    }

    private boolean checkEntryPath() {
        try {
            return new File(META_INF).getCanonicalPath().equals(new File(META_INF_DOT).getCanonicalPath());
        } catch (IOException e) {
            return true;
        }
    }

    public String getBundleLocationCanonicalPath() {
        return this.bundleLocationCanonicalPath;
    }

    public boolean isBundleLocationDirectory() {
        return this.isBundleLocationDirectory;
    }
}
