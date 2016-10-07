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

package org.eclipse.gemini.web.test.tomcat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Set;

import javax.servlet.ServletContext;

import org.eclipse.gemini.web.core.spi.ServletContainer;
import org.eclipse.gemini.web.core.spi.WebApplicationHandle;
import org.eclipse.virgo.test.framework.OsgiTestRunner;
import org.eclipse.virgo.test.framework.TestFrameworkUtils;
import org.eclipse.virgo.util.io.FileCopyUtils;
import org.eclipse.virgo.util.io.FileSystemUtils;
import org.eclipse.virgo.util.io.IOUtils;
import org.eclipse.virgo.util.io.PathReference;
import org.eclipse.virgo.util.io.ZipUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

@RunWith(OsgiTestRunner.class)
public class TomcatServletContainerTests {

    private static final String PATH_WAR_WITH_TLD_WAR = "../org.eclipse.gemini.web.test/target/resources/war-with-tld.war?Web-ContextPath=/war-with-tld";

    private static final String PATH_WAR_WITH_SERVLET = "../org.eclipse.gemini.web.test/target/resources/war-with-servlet.war?Web-ContextPath=/war-with-servlet";

    private static final String LOCATION_PREFIX = "webbundle:file:";

    private static final String LOCATION_SIMPLE_WAR = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.core/target/resources/simple-war.war?Web-ContextPath=/simple-war";

    private static final String LOCATION_WAR_WITH_SERVLET = "webbundle:file:" + PATH_WAR_WITH_SERVLET;

    private static final String LOCATION_WAR_WITH_JSP = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.test/target/resources/war-with-jsp.war?Web-ContextPath=/war-with-jsp";

    private static final String LOCATION_WAR_WITH_TLD = LOCATION_PREFIX + PATH_WAR_WITH_TLD_WAR;

    private static final String LOCATION_WAR_WITH_TLD_FROM_DEPENDENCY = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.test/src/test/resources/war-with-tld-from-dependency.war?Web-ContextPath=/war-with-tld-from-dependency";

    private static final String LOCATION_WAR_WITH_TLD_IMPORT_SYSTEM_PACKAGES = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.test/src/test/resources/war-with-tld-import-system-packages.war?Web-ContextPath=/war-with-tld-import-system-packages";

    private static final String LOCATION_WAR_WITH_CONTEXT_XML_RESOURCES = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.test/target/resources/war-with-context-xml-custom-classloader.war?Web-ContextPath=/war-with-context-xml-custom-classloader";

    private static final String LOCATION_WAR_WITH_CONTEXT_XML_CROSS_CONTEXT = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.test/target/resources/war-with-context-xml-cross-context.war?Web-ContextPath=/war-with-context-xml-cross-context";

    private static final String LOCATION_WAR_WITH_WEB_XML_FROM_FRAGMENT = "file:../org.eclipse.gemini.web.test/target/resources/war-with-web-xml-from-fragment.war";

    private static final String LOCATION_FRAGMENT_PROVIDES_WEB_XML = "file:../org.eclipse.gemini.web.test/target/resources/fragment-provides-web-xml.jar";

    private static final String LOCATION_WAR_WITH_ANNOTATIONS = "../org.eclipse.gemini.web.test/target/resources/war-with-annotations.war?Web-ContextPath=/war-with-annotations";

    private static final String LOCATION_BUNDLE_CUSTOMIZER = "file:../org.eclipse.gemini.web.test/target/resources/customizer-bundle.jar";

    private static final String LOCATION_WAR_WITH_RESOURCE_REFERENCES = "../org.eclipse.gemini.web.test/target/resources/war-with-resource-references.war?Web-ContextPath=/war-with-resource-references";

    private BundleContext bundleContext;

    private ServletContainer container;

    private static final String IVY_CACHE = System.getProperty("ivy.cache");

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("org.eclipse.gemini.web.tomcat.config.path", "target/config/tomcat-server.xml");
    }

    @Before
    public void before() throws Exception {
        this.bundleContext = TestFrameworkUtils.getBundleContextForTestClass(getClass());
        ServiceReference<?> ref = this.bundleContext.getServiceReference(ServletContainer.class.getName());
        this.container = (ServletContainer) this.bundleContext.getService(ref);
    }

    @Test
    public void testServletContainerAvailable() {
        assertNotNull(this.container);
        Socket socket = null;
        try {
            socket = new Socket("localhost", 8080);
        } catch (UnknownHostException e) {
            fail("Unable to connect");
        } catch (IOException e) {
            fail("Unable to connect");
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Test
    public void testInstallSimpleWar() throws Exception {
        String location = LOCATION_SIMPLE_WAR;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        validateNotFound("http://localhost:8080/test/index.html");

        WebApplicationHandle handle = this.container.createWebApplication("/test", bundle);
        try {
            this.container.startWebApplication(handle);
            assertNotNull(handle);

            validateURL("http://localhost:8080/test/index.html");
            validateNotFound("http://localhost:8080/test/META-INF./MANIFEST.MF");
        } finally {
            this.container.stopWebApplication(handle);
        }

        validateNotFound("http://localhost:8080/test/index.html");

    }

    @Test
    public void testWarWithServlet() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_SERVLET, "/war-with-servlet");
        try {
            validateURL("http://localhost:8080/war-with-servlet/test");
            validateURLExpectedContent("http://localhost:8080/war-with-servlet/", new String[] { "path info: /", "servlet path: ", "context path: " });
            validateURLExpectedContent("http://localhost:8080/war-with-servlet/alabala", new String[] { "path info: null", "servlet path: /alabala",
                "context path: /war-with-servlet" });
            validateURLExpectedContent("http://localhost:8080/war-with-servlet/test.jsp", new String[] { "Found resources 2" });
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void testWarWithBasicJSP() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_JSP, "/war-with-jsp");
        try {
            validateURL("http://localhost:8080/war-with-jsp/index.jsp");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void testWarWithJSTL() throws Exception {
        testWarWithJSTL("");
    }

    private void testWarWithJSTL(String addtionalUrlSuffix) throws MalformedURLException, IOException, BundleException {
        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_TLD + addtionalUrlSuffix, "/war-with-tld");
        try {
            validateURL("http://localhost:8080/war-with-tld/test.jsp");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void testWarWithJSTLFromDependency() throws MalformedURLException, IOException, BundleException {
        testJSTL(false);
    }

    @Test
    public void testWarWithJSTLFromExplodedDependency() throws MalformedURLException, IOException, BundleException {
        testJSTL(true);
    }

    private void testJSTL(boolean exploded) throws BundleException, MalformedURLException, IOException {
        String jstlLocation;
        PathReference unzippedJstl = null;
        if (!exploded) {
            jstlLocation = "file:" + IVY_CACHE
                + "/repository/org.eclipse.virgo.mirrored/javax.servlet.jsp.jstl/1.2.0.v201105211821/javax.servlet.jsp.jstl-1.2.0.v201105211821.jar";
        } else {
            String jstlPath = IVY_CACHE
                + "/repository/org.eclipse.virgo.mirrored/javax.servlet.jsp.jstl/1.2.0.v201105211821/javax.servlet.jsp.jstl-1.2.0.v201105211821.jar";
            PathReference jstl = new PathReference(jstlPath);
            unzippedJstl = explode(jstl);

            jstlLocation = "file:" + unzippedJstl.getAbsolutePath();
        }
        Bundle jstlBundle = this.bundleContext.installBundle(jstlLocation);

        try {
            Bundle bundle = this.bundleContext.installBundle(LOCATION_WAR_WITH_TLD_FROM_DEPENDENCY);

            try {
                bundle.start();

                WebApplicationHandle handle = this.container.createWebApplication("/war-with-tld-from-dependency", bundle);
                this.container.startWebApplication(handle);

                try {
                    validateURL("http://localhost:8080/war-with-tld-from-dependency/test.jsp");
                } finally {
                    this.container.stopWebApplication(handle);
                }
            } finally {
                bundle.uninstall();
            }
        } finally {
            jstlBundle.uninstall();
            if (exploded && unzippedJstl != null) {
                unzippedJstl.delete(true);
            }
        }
    }

    @Test
    public void testWarWithJSTLThatImportsSystemPackages() throws MalformedURLException, IOException, BundleException {
        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_TLD_IMPORT_SYSTEM_PACKAGES, "/war-with-tld");
        try {
            validateURL("http://localhost:8080/war-with-tld/test.jsp");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void testGetRealPathWithJarBundle() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_SERVLET, "/war-with-servlet");
        try {
            ServletContext context = ((WebApplicationHandle) result[1]).getServletContext();
            assertNotNull(context);

            String path = context.getRealPath("/WEB-INF/web.xml");
            assertNull(path);
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void testServletContextResourceLookup() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_SERVLET, "/war-with-servlet");
        try {
            ServletContext context = ((WebApplicationHandle) result[1]).getServletContext();
            assertNotNull(context);

            URL resource = context.getResource("/WEB-INF/web.xml");
            assertNotNull(resource);

            URLConnection connection = resource.openConnection();
            assertNotNull(connection);

            Set<?> paths = context.getResourcePaths("/WEB-INF");
            assertNotNull(paths);
            assertEquals(3, paths.size());

        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void rootContextPath() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_SERVLET, "");
        try {
            ServletContext context = ((WebApplicationHandle) result[1]).getServletContext();
            assertEquals("", context.getContextPath());
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    private void validateURL(String path) throws MalformedURLException, IOException {
        boolean error = false;
        InputStream stream = null;
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            stream = conn.getInputStream();
        } else {
            stream = conn.getErrorStream();
            error = true;
        }
        assertNotNull(stream);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        assertFalse(error);
    }

    private void validateURLExpectedContent(String path, String... extectedContent) throws MalformedURLException, IOException {
        boolean error = false;
        InputStream stream = null;
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            stream = conn.getInputStream();
        } else {
            stream = conn.getErrorStream();
            error = true;
        }
        assertNotNull(stream);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        String content = stringBuilder.toString();
        System.out.println(content);
        assertFalse(error);
        for (int j = 0; j < extectedContent.length; j++) {
            assertTrue(content.contains(extectedContent[j]));
        }
    }

    private void validateNotFound(String path) throws Exception {
        URL url = new URL(path);
        try {
            url.openConnection().getInputStream();
        } catch (IOException e) {
            assertTrue("success case", true);
            return;
        }
        fail("URL '" + path + "' is still deployed");
    }

    private PathReference explode(PathReference packed) throws IOException {
        PathReference target = new PathReference("target");
        return ZipUtils.unzipTo(packed, target);
    }

    @Test
    public void testLastModified() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_SERVLET, "");
        try {
            ServletContext context = ((WebApplicationHandle) result[1]).getServletContext();
            URL resource = context.getResource("/META-INF/");
            long lm = 0;
            if (resource != null) {
                lm = resource.openConnection().getLastModified();
            }
            assertTrue(lm != 0);
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void testWarWithContextXml() throws Exception {
        // Copy default context.xml
        File defaultContextXml = new File("target/config/context.xml");
        createFileWithContent(defaultContextXml, "<Context crossContext=\"true\"/>");

        // Copy default context.xml.default
        File defaultHostContextXml = new File("target/config/Catalina/localhost/context.xml.default");
        String content = "<Context>"
            + "<Resource name=\"mail/Session1\" auth=\"Container\" type=\"javax.mail.Session\" mail.smtp.host=\"localhost\"/>" + "</Context>";
        createFileWithContent(defaultHostContextXml, content);

        File tomcatServerXml = new File("target/config/tomcat-server.xml");
        createFileWithContent(tomcatServerXml, "");

        String location1 = LOCATION_WAR_WITH_CONTEXT_XML_RESOURCES;
        Bundle bundle1 = this.bundleContext.installBundle(location1);
        bundle1.start();

        String location2 = LOCATION_WAR_WITH_CONTEXT_XML_CROSS_CONTEXT;
        Bundle bundle2 = this.bundleContext.installBundle(location2);
        bundle2.start();

        WebApplicationHandle handle1 = this.container.createWebApplication("/war-with-context-xml-custom-classloader", bundle1);
        this.container.startWebApplication(handle1);

        WebApplicationHandle handle2 = this.container.createWebApplication("/war-with-context-xml-cross-context", bundle2);
        this.container.startWebApplication(handle2);
        try {
            // tests custom classloader, access log valve and basic authenticator
            // all specified in context.xml
            validateURL("http://localhost:8080/war-with-context-xml-custom-classloader/index.html");

            // tests JNDI resources
            // tests cross context functionality
            validateURL("http://localhost:8080/war-with-context-xml-cross-context/index.jsp");
            validateURL("http://localhost:8080/war-with-context-xml-cross-context/forward.jsp");
        } finally {
            this.container.stopWebApplication(handle1);
            bundle1.uninstall();

            this.container.stopWebApplication(handle2);
            bundle2.uninstall();

            assertTrue(defaultContextXml.delete());
            assertTrue(defaultHostContextXml.delete());
            assertTrue(tomcatServerXml.delete());
        }
    }

    @Test
    public void testInstallWebAppDir() throws Exception {
        // Create web app dir
        File webAppDir = new File("target/test-classes/simple-web-app-dir");
        File indexJsp = new File(webAppDir, "index.jsp");
        createFileWithContent(indexJsp, "Hello World!\n"
            + "config.getServletContext().getResourcePaths(/): <%=config.getServletContext().getResourcePaths(\"/\")%>\n"
            + "config.getServletContext().getRealPath(/): <%=config.getServletContext().getRealPath(\"/\")%>\n"
            + "**************  REAL PATH: <%=request.getRealPath(\".\")%>\n"
            + "**************  REAL PATH: <%=request.getRealPath(\"META-INF/.\")%>\n");
        File metaInf = new File(webAppDir, "META-INF");
        File manifest = new File(metaInf, "MANIFEST.MF");
        createFileWithContent(manifest, "");
        File otherMetaInf = new File(webAppDir, "blah/META-INF.");
        File otherManifest = new File(otherMetaInf, "MANIFEST.MF");
        createFileWithContent(otherManifest, "Manifest-Version: 1.0");
        File otherDirectory = new File(webAppDir, "blah/META-INF.blah");
        File otherStaticResource = new File(otherDirectory, "test.txt");
        createFileWithContent(otherStaticResource, "TEST");

        Object[] result = startWebApplicationWith(LOCATION_PREFIX + webAppDir.getAbsolutePath() + "?Web-ContextPath=/simple-web-app-dir",
            "/simple-web-app-dir");

        try {
            validateURL("http://localhost:8080/simple-web-app-dir/index.jsp");
            validateNotFound("http://localhost:8080/simple-web-app-dir/META-INF./MANIFEST.MF");
            validateURLExpectedContent("http://localhost:8080/simple-web-app-dir/blah/META-INF./MANIFEST.MF", "Manifest-Version: 1.0");
            validateURLExpectedContent("http://localhost:8080/simple-web-app-dir/blah/META-INF.blah/test.txt", "TEST");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
            FileSystemUtils.deleteRecursively(webAppDir);
            FileSystemUtils.deleteRecursively(new File("temp"));
        }
    }

    @Test
    public void testWarWithWebXmlFromFragment() throws Exception {
        Bundle fragment = this.bundleContext.installBundle(LOCATION_FRAGMENT_PROVIDES_WEB_XML);

        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_WEB_XML_FROM_FRAGMENT, "/war-with-web-xml-from-fragment");
        try {
            validateURL("http://localhost:8080/war-with-web-xml-from-fragment");
            validateURL("http://localhost:8080/war-with-web-xml-from-fragment/NewPackageServlet");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
            fragment.uninstall();
        }
    }

    @Test
    public void testWarWithAnnotations() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_PREFIX + LOCATION_WAR_WITH_ANNOTATIONS, "/war-with-annotations");
        try {
            validateURL("http://localhost:8080/war-with-annotations/TestServlet");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void testStaticResourceInNestedJar() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_SERVLET, "/war-with-servlet");
        try {
            validateURLExpectedContent("http://localhost:8080/war-with-servlet/meta_inf_resource.jsp", "TEST");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void testCustomizers() throws Exception {
        Bundle customizer = this.bundleContext.installBundle(LOCATION_BUNDLE_CUSTOMIZER);
        customizer.start();

        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_SERVLET, "/war-with-servlet");
        try {
            validateURL("http://localhost:8080/war-with-servlet/CustomServlet");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
            customizer.uninstall();
        }
    }

    @Test
    public void testWarWithResourceRefereces() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_PREFIX + LOCATION_WAR_WITH_RESOURCE_REFERENCES, "/war-with-resource-references");
        try {
            validateURLExpectedContent("http://localhost:8080/war-with-resource-references/Bug52792Servlet",
                "Name [unknown-resource] is not bound in this Context. Unable to find [unknown-resource].");
            validateURLExpectedContent("http://localhost:8080/war-with-resource-references/Bug52974Servlet", new String[] {
                "@Resource injection - field: resource", "@Resource injection - method: resource1" });
            validateURLExpectedContent("http://localhost:8080/war-with-resource-references/Bug53180Servlet", "Resource: resource");
            validateURLExpectedContent("http://localhost:8080/war-with-resource-references/Bug53333Servlet", new String[] { "resource1: 1",
                "resource2: 2" });
            validateURLExpectedContent("http://localhost:8080/war-with-resource-references/Bug53090Servlet",
                "Resource injection: super class: resource");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    @Test
    public void testServletContainerWithCustomDefaultWebXml() throws Exception {
        File tomcatServerXml = new File("target/config/tomcat-server.xml");
        createFileWithContent(tomcatServerXml, "");

        // In this custom default web.xml the directory listing is enabled
        // Thus we will ensure that a custom default web.xml is used
        File defaultWebXml = new File("target/config/web.xml");
        createFileWithContent(
            defaultWebXml,
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
                + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"\nversion=\"2.5\">\n"
                + "<servlet>\n<servlet-name>default</servlet-name>\n<servlet-class>org.apache.catalina.servlets.DefaultServlet</servlet-class>\n"
                + "<init-param><param-name>debug</param-name><param-value>0</param-value></init-param>\n"
                + "<init-param><param-name>listings</param-name><param-value>true</param-value></init-param>\n"
                + "<load-on-startup>1</load-on-startup>\n</servlet>\n"
                + "<servlet-mapping>\n<servlet-name>default</servlet-name>\n<url-pattern>/</url-pattern>\n</servlet-mapping>\n"
                + "<filter>\n<filter-name>requestdumper</filter-name>\n<filter-class>org.apache.catalina.filters.RequestDumperFilter</filter-class>\n</filter>\n"
                + "<filter-mapping>\n<filter-name>requestdumper</filter-name>\n<url-pattern>/*</url-pattern>\n</filter-mapping>\n</web-app>");

        Object[] result = startWebApplicationWith(LOCATION_WAR_WITH_TLD, "/war-with-tld");
        try {
            validateURLExpectedContent("http://localhost:8080/war-with-tld", new String[] { "test.jsp", "0.2 kb" });
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();

            assertTrue(tomcatServerXml.delete());
            FileCopyUtils.copy(new File("src/test/resources/web.xml"), defaultWebXml);
        }
    }

    @Test
    public void testDirectoryListing() throws Exception {
        File tomcatServerXml = new File("target/config/tomcat-server.xml");
        createFileWithContent(tomcatServerXml, "");

        // In this custom default web.xml the directory listing is enabled
        // Thus we will ensure that a custom default web.xml is used
        File defaultWebXml = new File("target/config/web.xml");
        createFileWithContent(defaultWebXml, "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
            + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"\nversion=\"2.5\">\n"
            + "<servlet>\n<servlet-name>default</servlet-name>\n<servlet-class>org.apache.catalina.servlets.DefaultServlet</servlet-class>\n"
            + "<init-param><param-name>debug</param-name><param-value>0</param-value></init-param>\n"
            + "<init-param><param-name>listings</param-name><param-value>true</param-value></init-param>\n"
            + "<load-on-startup>1</load-on-startup>\n</servlet>\n"
            + "<servlet-mapping><servlet-name>default</servlet-name><url-pattern>/</url-pattern></servlet-mapping></web-app>");

        // Create web app dir
        File webAppDir = new File("target/test-classes/simple-web-app-dir");
        File testFolder = new File(webAppDir, "test");
        File testHtml = new File(testFolder, "test.html");
        createFileWithContent(testHtml, "Hello World!");
        File metaInf = new File(webAppDir, "META-INF");
        File manifest = new File(metaInf, "MANIFEST.MF");
        createFileWithContent(manifest, "Manifest-Version: 1.0\n" + "Bundle-ManifestVersion: 2\n" + "Web-ContextPath: /simple-web-app-dir\n"
            + "Bundle-SymbolicName: simple-web-app-dir\n\n");

        Object[] result = startWebApplicationWith("reference:file:" + webAppDir.getAbsolutePath(), "/simple-web-app-dir");
        try {
            validateURL("http://localhost:8080/simple-web-app-dir/test/test.html");
            validateURL("http://localhost:8080/simple-web-app-dir/test");

            FileSystemUtils.deleteRecursively(testFolder);
            // there is cacheTTL property which default value is 5s
            Thread.sleep(5000);
            validateNotFound("http://localhost:8080/simple-web-app-dir/test");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();

            FileSystemUtils.deleteRecursively(webAppDir);
            assertTrue(tomcatServerXml.delete());
            FileCopyUtils.copy(new File("src/test/resources/web.xml"), defaultWebXml);
        }
    }

    private void createFileWithContent(File file, String content) throws Exception {
        if (!file.getParentFile().exists()) {
            assertTrue(file.getParentFile().mkdirs());
        }
        FileWriter fWriter = null;
        try {
            fWriter = new FileWriter(file);
            fWriter.write(content);
            fWriter.flush();
        } finally {
            IOUtils.closeQuietly(fWriter);
        }
    }

    private Object[] startWebApplicationWith(String location, String contextPath) throws BundleException {
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();
        assertNotNull(bundle.getSymbolicName());

        WebApplicationHandle handle = this.container.createWebApplication(contextPath, bundle);
        this.container.startWebApplication(handle);
        return new Object[] { bundle, handle };
    }
}
