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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javax.servlet.ServletContext;

import org.apache.tomcat.Jar;
import org.apache.tomcat.JarScannerCallback;
import org.eclipse.gemini.web.tomcat.internal.loader.BundleWebappClassLoader;
import org.eclipse.gemini.web.tomcat.internal.support.BundleDependencyDeterminer;
import org.eclipse.gemini.web.tomcat.internal.support.BundleFileResolver;
import org.eclipse.gemini.web.tomcat.spi.ClassLoaderCustomizer;
import org.eclipse.virgo.test.stubs.framework.StubBundle;
import org.eclipse.virgo.test.stubs.framework.StubBundleContext;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class BundleDependenciesJarScannerTests {

    private final BundleDependencyDeterminer dependencyDeterminer = createMock(BundleDependencyDeterminer.class);

    private final BundleFileResolver bundleFileResolver = createMock(BundleFileResolver.class);

    private final StubBundleContext bundleContext = new StubBundleContext();

    private final BundleDependenciesJarScanner scanner = new BundleDependenciesJarScanner(this.dependencyDeterminer, this.bundleFileResolver,
        this.bundleContext);

    private final Bundle bundle = new StubBundle();

    private final JarScannerCallback callback = createMock(JarScannerCallback.class);

    private final ClassLoaderCustomizer classLoaderCustomizer = createNiceMock(ClassLoaderCustomizer.class);

    private final Bundle dependency = createMock(Bundle.class);

    private final ServletContext servletContext = createMock(ServletContext.class);

    @Test
    public void noDependencies() throws IOException {
        expect(this.dependencyDeterminer.getDependencies(this.bundle)).andReturn(Collections.<Bundle> emptySet());

        ClassLoader classLoader = new BundleWebappClassLoader(this.bundle, this.classLoaderCustomizer);
        expect(this.servletContext.getClassLoader()).andReturn(classLoader);

        replay(this.dependencyDeterminer, this.servletContext);

        this.scanner.scan(null, this.servletContext, this.callback);

        ((URLClassLoader) classLoader).close();

        verify(this.dependencyDeterminer, this.servletContext);
    }

    @Test
    public void scanDirectory() throws IOException {
        expect(this.dependencyDeterminer.getDependencies(this.bundle)).andReturn(new HashSet<>(Arrays.asList(this.dependency)));

        File dependencyFile = new File("src/test/resources");
        expect(this.bundleFileResolver.resolve(this.dependency)).andReturn(dependencyFile);
        this.callback.scan(dependencyFile, null, true);

        ClassLoader classLoader = new BundleWebappClassLoader(this.bundle, this.classLoaderCustomizer);
        expect(this.servletContext.getClassLoader()).andReturn(classLoader);

        replay(this.dependencyDeterminer, this.bundleFileResolver, this.callback, this.servletContext);

        this.scanner.scan(null, this.servletContext, this.callback);

        ((URLClassLoader) classLoader).close();

        verify(this.dependencyDeterminer, this.bundleFileResolver, this.callback, this.servletContext);
    }

    @Test
    public void scanFile() throws IOException {
        expect(this.dependencyDeterminer.getDependencies(this.bundle)).andReturn(new HashSet<>(Arrays.asList(this.dependency)));

        File dependencyFile = new File("./src/test/resources/bundle.jar");
        expect(this.bundleFileResolver.resolve(this.dependency)).andReturn(dependencyFile);
        this.callback.scan(isA(Jar.class), (String) isNull(), eq(true));

        ClassLoader classLoader = new BundleWebappClassLoader(this.bundle, this.classLoaderCustomizer);
        expect(this.servletContext.getClassLoader()).andReturn(classLoader);

        replay(this.dependencyDeterminer, this.bundleFileResolver, this.callback, this.servletContext);

        this.scanner.scan(null, this.servletContext, this.callback);

        ((URLClassLoader) classLoader).close();

        verify(this.dependencyDeterminer, this.bundleFileResolver, this.callback, this.servletContext);
    }

    @Test
    public void scanJarUrlConnection() throws IOException {
        expect(this.dependencyDeterminer.getDependencies(this.bundle)).andReturn(new HashSet<>(Arrays.asList(this.dependency)));
        URL bundleLocation = new File("./src/test/resources/bundle.jar").toURI().toURL();
        expect(this.dependency.getEntry("/")).andReturn(bundleLocation);
        expect(this.dependency.getSymbolicName()).andReturn("bundle");

        expect(this.bundleFileResolver.resolve(this.dependency)).andReturn(null);
        this.callback.scan(isA(Jar.class), (String) isNull(), eq(true));

        ClassLoader classLoader = new BundleWebappClassLoader(this.bundle, this.classLoaderCustomizer);
        expect(this.servletContext.getClassLoader()).andReturn(classLoader);

        replay(this.dependencyDeterminer, this.bundleFileResolver, this.callback, this.dependency, this.servletContext);

        this.scanner.scan(null, this.servletContext, this.callback);

        ((URLClassLoader) classLoader).close();

        verify(this.dependencyDeterminer, this.bundleFileResolver, this.callback, this.dependency, this.servletContext);
    }
}
