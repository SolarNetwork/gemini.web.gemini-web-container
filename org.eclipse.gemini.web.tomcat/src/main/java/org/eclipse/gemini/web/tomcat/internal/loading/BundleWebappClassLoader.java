/*******************************************************************************
 * Copyright (c) 2009, 2010 VMware Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.loader.Constants;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.res.StringManager;
import org.eclipse.gemini.web.tomcat.spi.ClassLoaderCustomizer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

public class BundleWebappClassLoader extends URLClassLoader implements Lifecycle, BundleReference {

    /**
     * The string manager for this package.
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);

    protected static final org.apache.juli.logging.Log log = org.apache.juli.logging.LogFactory.getLog(BundleWebappClassLoader.class);

    /**
     * Has this class loader been started?
     */
    private volatile boolean started = false;

    /**
     * The delegating class loader for the web application's bundle(s).
     */
    private final ClassLoader bundleDelegatingClassLoader;

    private final ClassLoaderCustomizer classLoaderCustomizer;

    private final Bundle bundle;

    // ------------------------------------------------------------------------
    // --- Constructors
    // ------------------------------------------------------------------------

    public BundleWebappClassLoader(final Bundle bundle, ClassLoaderCustomizer classLoaderCustomizer) {
        super(new URL[0]);
        this.bundle = bundle;
        this.classLoaderCustomizer = classLoaderCustomizer;

        addBundleClassPathURLs(bundle);

        this.bundleDelegatingClassLoader = createClassLoaderChain(bundle);
    }

    private ChainedClassLoader createClassLoaderChain(Bundle bundle) {
        ClassLoader[] loaders = { 
            BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle), 
            Context.class.getClassLoader() // catalina classloader
        };

        ClassLoader[] chainExtensions = this.classLoaderCustomizer.extendClassLoaderChain(bundle);

        ClassLoader[] finalLoaders;
        if (chainExtensions != null && chainExtensions.length > 0) {
            finalLoaders = new ClassLoader[loaders.length + chainExtensions.length];
            System.arraycopy(loaders, 0, finalLoaders, 0, loaders.length);
            System.arraycopy(chainExtensions, 0, finalLoaders, loaders.length, chainExtensions.length);
        } else {
            finalLoaders = loaders;
        }
        return ChainedClassLoader.create(finalLoaders);
    }

    private void addBundleClassPathURLs(Bundle bundle) {
        Set<URI> uris = BundleClassPathURLExtractor.extractBundleClassPathURLs(bundle);
        for (URI uri : uris) {
            try {
                addURL(uri.toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    // -------------------------------------------------------------------------
    // --- Instrumentation
    // -------------------------------------------------------------------------

    public void addTransformer(ClassFileTransformer transformer) {
        this.classLoaderCustomizer.addClassFileTransformer(transformer, this.bundle);
    }

    public ClassLoader getThrowawayClassLoader() {
        return this.classLoaderCustomizer.createThrowawayClassLoader(this.bundle);
    }

    // -------------------------------------------------------------------------
    // --- Lifecycle
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void addLifecycleListener(LifecycleListener listener) {
        /* no-op */
    }

    /**
     * {@inheritDoc}
     */
    public LifecycleListener[] findLifecycleListeners() {
        return new LifecycleListener[0];
    }

    /**
     * {@inheritDoc}
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        /* no-op */
    }

    @Override
    public void destroy() throws LifecycleException {
        /* no-op */
    }

    @Override
    public LifecycleState getState() {
        return LifecycleState.NEW;
    }

    @Override
    public String getStateName() {
        return getState().toString();
    }

    @Override
    public void init() throws LifecycleException {
        /* no-op */
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws LifecycleException {
        this.started = true;
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws LifecycleException {
        // Clearing references should be done before setting started to
        // false, due to possible side effects
        clearReferences();

        this.started = false;
    }

    // -------------------------------------------------------------------------
    // --- ClassLoader
    // -------------------------------------------------------------------------

    /**
     * Find the resource with the given name. A resource is some data (images, audio, text, etc.) that can be accessed
     * by class code in a way that is independent of the location of the code. The name of a resource is a "/"-separated
     * path name that identifies the resource. If the resource cannot be found, return <code>null</code>.
     * <p>
     * This method searches according to the following algorithm, returning as soon as it finds the appropriate URL. If
     * the resource cannot be found, returns <code>null</code>.
     * <ul>
     * <li>???</li>
     * </ul>
     * 
     * @param name Name of the resource to return a URL for
     */
    @Override
    public URL getResource(String name) {
        if (log.isDebugEnabled()) {
            log.debug("getResource(" + name + ")");
        }

        URL url = null;

        url = this.bundleDelegatingClassLoader.getResource(name);
        if (url != null) {
            return url;
        }

        // Resource was not found
        if (log.isDebugEnabled()) {
            log.debug("  --> Resource not found, returning null");
        }

        return null;
    }

    public InputStream getResourceAsStream(String name) {
        return super.getResourceAsStream(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("getResources(" + name + ")");
        }

        return this.bundleDelegatingClassLoader.getResources(name);
    }

    /**
     * Load the class with the specified name, searching using the following algorithm until it finds and returns the
     * class. If the class cannot be found, throws <code>ClassNotFoundException</code>.
     * <ul>
     * <li>Check cache of previously loaded classes</li>
     * <li>If not found, attempt to load the class from the Application's synthetic context bundle (and indirectly from
     * the Web module's bundle)</li>
     * <li>If not found, attempt to load the class from Tomcat's bundles</li>
     * <li>If not found, throw a <code>ClassNotFoundException</code></li>
     * </ul>
     * If the class was found using the above steps, and the <code>resolve</code> flag is <code>true</code>, this method
     * will then call <code>resolveClass(Class)</code> on the resulting Class object.
     * 
     * @param name Name of the class to be loaded
     * @param resolve If <code>true</code> then resolve the class
     * 
     * @exception ClassNotFoundException if the class was not found
     */
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (log.isDebugEnabled()) {
            log.debug("loadClass(" + name + ", " + resolve + ")");
        }

        Class<?> clazz = null;

        // Log access to stopped classloader
        if (!this.started) {
            try {
                throw new IllegalStateException();
            } catch (IllegalStateException e) {
                log.info(sm.getString("webappClassLoader.stopped", name), e);
            }
        }

        // Check our previously loaded class cache
        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (log.isDebugEnabled()) {
                log.debug("  Returning class from cache");
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        // Search the application's bundle
        if (log.isDebugEnabled()) {
            log.debug("  Searching the application's bundle");
        }
        try {
            clazz = this.bundleDelegatingClassLoader.loadClass(name);
            if (clazz != null) {
                if (log.isDebugEnabled()) {
                    log.debug("  Loading class from the delegating classloader");
                }
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            /* no-op */
        }

        throw new ClassNotFoundException(name);
    }

    // -------------------------------------------------------------------------
    // --- Protected Methods
    // -------------------------------------------------------------------------

    /**
     * Clear references.
     */
    protected void clearReferences() {
        // TODO think about more references that have to be cleared
        // Bug 345938 - BundleWebappClassLoader.clearReferences() - extend current functionality

        // Unregister any JDBC drivers loaded by this classloader
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = (Driver) drivers.nextElement();
            if (driver.getClass().getClassLoader() == this) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    log.warn("SQL driver deregistration failed", e);
                }
            }
        }

        // XXX Determine if we need to null out static or final fields from loaded classes as in WebappClassLoader

        // Clear the IntrospectionUtils cache.
        IntrospectionUtils.clear();

        // Clear the classloader reference in common-logging
        org.apache.juli.logging.LogFactory.release(this);

        // Clear the classloader reference in the VM's bean introspector
        java.beans.Introspector.flushCaches();
    }

    public Bundle getBundle() {
        return this.bundle;
    }
}
