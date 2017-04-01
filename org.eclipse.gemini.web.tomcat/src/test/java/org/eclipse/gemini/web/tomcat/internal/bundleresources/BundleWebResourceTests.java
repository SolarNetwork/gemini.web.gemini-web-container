/*******************************************************************************
 * Copyright (c) 2015, 2017 SAP SE
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

package org.eclipse.gemini.web.tomcat.internal.bundleresources;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.catalina.WebResourceRoot;
import org.eclipse.gemini.web.tomcat.internal.loader.FindEntriesDelegateImpl;
import org.eclipse.virgo.test.stubs.framework.StubBundle;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class BundleWebResourceTests {

    private final StubBundle testBundle = new StubBundle();

    @Before
    public void createEntries() throws MalformedURLException {
        this.testBundle.addEntryPaths("", createPathsEnumeration("sub/"));
        this.testBundle.addEntryPaths("sub/", createPathsEnumeration("sub/one.txt", "sub/another.sub/"));
        this.testBundle.addEntryPaths("sub/another.sub/", createPathsEnumeration("sub/another.sub/two.txt"));

        this.testBundle.addEntry("", new File("src/test/resources/").toURI().toURL());
        this.testBundle.addEntry("sub/", new File("src/test/resources/sub/").toURI().toURL());
        this.testBundle.addEntry("sub/one.txt", new File("src/test/resources/sub/one.txt").toURI().toURL());
        this.testBundle.addEntry("sub/another.sub/", new File("src/test/resources/sub/another.sub/").toURI().toURL());
        this.testBundle.addEntry("sub/another.sub/two.txt", new File("src/test/resources/sub/another.sub/two.txt").toURI().toURL());
        this.testBundle.addEntry("a/", new File("src/test/resources/a/").toURI().toURL());
        this.testBundle.addEntry("a/b/", new File("src/test/resources/a/b/").toURI().toURL());
        this.testBundle.addEntry("a/b/c.txt", new File("src/test/resources/a/b/c.txt").toURI().toURL());

        this.testBundle.addEntry("/", new File("src/test/resources/").toURI().toURL());
        this.testBundle.addEntry("/sub/", new File("src/test/resources/sub/").toURI().toURL());
        this.testBundle.addEntry("/sub/one.txt", new File("src/test/resources/sub/one.txt").toURI().toURL());
        this.testBundle.addEntry("/sub/another.sub/", new File("src/test/resources/sub/another.sub/").toURI().toURL());
        this.testBundle.addEntry("/sub/another.sub/two.txt", new File("src/test/resources/sub/another.sub/two.txt").toURI().toURL());
        this.testBundle.addEntry("/a/", new File("src/test/resources/a/").toURI().toURL());
        this.testBundle.addEntry("/a/b/", new File("src/test/resources/a/b/").toURI().toURL());
        this.testBundle.addEntry("/a/b/c.txt", new File("src/test/resources/a/b/c.txt").toURI().toURL());
        this.testBundle.setFindEntriesDelegate(new FindEntriesDelegateImpl(this.testBundle));
    }

    @Test
    public void testList() {
        testList(this.testBundle);
    }

    @Test
    public void testListBundleWithFragment() throws Exception {
        Bundle bundle = createMock(Bundle.class);
        BundleRevision bundleRevision = createMock(BundleRevision.class);
        BundleWiring bundleWiring = createMock(BundleWiring.class);
        BundleWire bundleWire = createMock(BundleWire.class);
        Bundle fbundle = createMock(Bundle.class);
        BundleRevision fbundleRevision = createMock(BundleRevision.class);
        BundleWiring fbundleWiring = createMock(BundleWiring.class);
        expect(bundle.getEntry("")).andReturn(Paths.get("src/test/resources/sub/").toUri().toURL()).anyTimes();
        expect(bundle.getEntry("/")).andReturn(Paths.get("src/test/resources/sub/").toUri().toURL()).anyTimes();
        expect(bundle.getEntry("sub/")).andReturn(Paths.get("src/test/resources/sub/").toUri().toURL()).anyTimes();
        expect(bundle.getEntryPaths("")).andReturn(createPathsEnumeration("sub/"));
        expect(bundle.getEntryPaths("sub/")).andReturn(createPathsEnumeration("sub/one.txt"));
        List<URL> entries = new ArrayList<>();
        entries.add(Paths.get("src/test/resources/sub/one.txt").toUri().toURL());
        expect(bundle.findEntries("sub", "one.txt", false)).andReturn(Collections.enumeration(entries)).anyTimes();
        expect(bundle.adapt(BundleRevision.class)).andReturn(bundleRevision);
        expect(bundleRevision.getWiring()).andReturn(bundleWiring);
        expect(bundleWiring.getProvidedWires(BundleRevision.HOST_NAMESPACE)).andReturn(Arrays.asList(new BundleWire[] { bundleWire }));
        expect(bundleWire.getRequirerWiring()).andReturn(fbundleWiring);
        expect(fbundleWiring.getRevision()).andReturn(fbundleRevision);
        expect(fbundleRevision.getBundle()).andReturn(fbundle);
        expect(bundle.getEntry("sub/another.sub/")).andReturn(Paths.get("src/test/resources/sub/another.sub/").toUri().toURL()).anyTimes();
        expect(fbundle.getEntryPaths("")).andReturn(createPathsEnumeration("sub/"));
        expect(fbundle.getEntryPaths("sub/")).andReturn(createPathsEnumeration("sub/another.sub/"));

        replay(bundle, bundleRevision, bundleWiring, bundleWire, fbundle, fbundleRevision, fbundleWiring);

        testList(bundle);

        verify(bundle, bundleRevision, bundleWiring, bundleWire, fbundle, fbundleRevision, fbundleWiring);
    }

    @Test
    public void testGetEntry() {
        WebResourceRoot root = createMock(WebResourceRoot.class);
        BundleWebResource entry = new BundleWebResource(this.testBundle, root);

        assertNotNull(entry.getEntry("sub/"));
        assertNotNull(entry.getEntry("sub/one.txt"));
        assertNotNull(entry.getEntry("sub/another.sub/"));
        assertNotNull(entry.getEntry("sub/another.sub/two.txt"));
        assertNotNull(entry.getEntry("."));
        assertNotNull(entry.getEntry("sub/."));
        assertNotNull(entry.getEntry(""));
        assertNotNull(entry.getEntry("/"));

        assertTrue(entry.getEntry("sub/").getKey().isDirectory());
        assertTrue(entry.getEntry("sub/another.sub/").getKey().isDirectory());
        assertTrue(entry.getEntry(".").getKey().isDirectory());
        assertTrue(entry.getEntry("sub/.").getKey().isDirectory());
        assertTrue(entry.getEntry("").getKey().isDirectory());
        assertTrue(entry.getEntry("/").getKey().isDirectory());
    }

    @Test
    public void testNames() {
        WebResourceRoot root = createMock(WebResourceRoot.class);
        BundleWebResource entry = new BundleWebResource(this.testBundle, root);

        Entry<BundleWebResource, URL> e = entry.getEntry("/");
        assertEquals("/", e.getKey().getName());

        e = entry.getEntry("/sub/");
        assertEquals("sub", e.getKey().getName());

        e = entry.getEntry("/sub/one.txt");
        assertEquals("one.txt", e.getKey().getName());

        e = entry.getEntry("");
        assertEquals("/", e.getKey().getName());

        e = entry.getEntry("sub/");
        assertEquals("sub", e.getKey().getName());

        e = entry.getEntry("sub/one.txt");
        assertEquals("one.txt", e.getKey().getName());

        e = entry.getEntry("/a/");
        assertEquals("a", e.getKey().getName());

        e = entry.getEntry("/a/b/");
        assertEquals("b", e.getKey().getName());

        e = entry.getEntry("/a/b/c.txt");
        assertEquals("c.txt", e.getKey().getName());

        e = entry.getEntry("a/");
        assertEquals("a", e.getKey().getName());

        e = entry.getEntry("a/b/");
        assertEquals("b", e.getKey().getName());

        e = entry.getEntry("a/b/c.txt");
        assertEquals("c.txt", e.getKey().getName());
    }

    private BundleWebResource findByPath(List<BundleWebResource> entries, String entry) {
        for (BundleWebResource bundleEntry : entries) {
            if (bundleEntry.getName().equals(entry)) {
                return bundleEntry;
            }
        }
        return null;
    }

    private Enumeration<String> createPathsEnumeration(String... paths) {
        Vector<String> vector = new Vector<>();

        for (String path : paths) {
            vector.add(path);
        }

        return vector.elements();
    }

    private void testList(Bundle bundle) {
        WebResourceRoot root = createMock(WebResourceRoot.class);
        BundleWebResource entry = new BundleWebResource(bundle, root);
        List<BundleWebResource> list = entry.list();

        BundleWebResource subEntry = findByPath(list, "sub");
        assertNotNull(subEntry);

        list = subEntry.list();
        assertNotNull(findByPath(list, "one.txt"));
        assertNotNull(findByPath(list, "another.sub"));
    }
}
