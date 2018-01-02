/*******************************************************************************
 * Copyright (c) 2014, 2018 SAP SE
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

import java.util.HashSet;
import java.util.Set;

import org.apache.catalina.core.StandardContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends the Tomcat {@link StandardContext} to add custom functionality.
 *
 *
 */
public class ExtendedStandardContext extends StandardContext {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExtendedStandardContext.class);

    private final Bundle bundle;

    public ExtendedStandardContext(Bundle bundle) {
        super();
        this.bundle = bundle;
    }

    @Override
    public synchronized void reload() {
        if (!getState().isAvailable()) {
            throw new IllegalStateException("Context with name [" + getName() + "] has not yet been started.");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Reloading Context with name [" + getName() + "] has started.");
        }

        final Bundle systemBundle = this.bundle.getBundleContext().getBundle(0);
        final FrameworkWiring frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
        Set<Bundle> bundles = new HashSet<>();
        bundles.add(this.bundle);
        frameworkWiring.refreshBundles(bundles);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Reloading Context with name [" + getName() + "] is completed");
        }

    }

    @Override
    public ClassLoader getParentClassLoader() {
        if (this.parentClassLoader != null) {
            return this.parentClassLoader;
        }

        this.parentClassLoader = getLoader().getClassLoader();
        if (this.parentClassLoader != null) {
            return this.parentClassLoader;
        }

        return super.getParentClassLoader();
    }

}
