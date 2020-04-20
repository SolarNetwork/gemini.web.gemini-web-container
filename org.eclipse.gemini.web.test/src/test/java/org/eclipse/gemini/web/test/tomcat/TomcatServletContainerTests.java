/*******************************************************************************
 * Copyright (c) 2009, 2020 VMware Inc.
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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.eclipse.gemini.web.core.spi.ServletContainer;
import org.eclipse.gemini.web.core.spi.WebApplicationHandle;
import org.eclipse.gemini.web.test.FileUtils;
import org.eclipse.virgo.test.framework.OsgiTestRunner;
import org.eclipse.virgo.test.framework.TestFrameworkUtils;
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

    private static final String LOCATION_WEBSOCKET = "../org.eclipse.gemini.web.test/target/resources/websocket.war?Web-ContextPath=/websocket";

    private BundleContext bundleContext;

    private ServletContainer container;

    private static final String IVY_CACHE = System.getProperty("ivy.cache");

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("org.eclipse.gemini.web.tomcat.config.path", "target/config/tomcat-server.xml");
        Path srcFile = Paths.get("src/test/resources/config/tomcat-server.xml");
        Path dstFile = Paths.get("target/config/tomcat-server.xml");
        if (Files.notExists(dstFile)) {
            Files.createDirectories(dstFile.getParent());
            Files.copy(srcFile, dstFile);
        }
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
        try (Socket socket = new Socket("localhost", 8080);) {
        } catch (IOException e) {
            fail("Unable to connect");
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
            validateURLExpectedContent("http://localhost:8080/war-with-servlet/test.jsp",
                new String[] { "Found resources 2", "Found input sources 3", "Found resource paths 3" });
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
        Path unzippedJstl = null;
        if (!exploded) {
            jstlLocation = "file:" + IVY_CACHE
                + "/repository/org.eclipse.virgo.mirrored/javax.servlet.jsp.jstl/1.2.0.v201105211821/javax.servlet.jsp.jstl-1.2.0.v201105211821.jar";
        } else {
            String jstlPath = IVY_CACHE
                + "/repository/org.eclipse.virgo.mirrored/javax.servlet.jsp.jstl/1.2.0.v201105211821/javax.servlet.jsp.jstl-1.2.0.v201105211821.jar";
            unzippedJstl = explode(Paths.get(jstlPath));

            jstlLocation = "file:" + unzippedJstl.toAbsolutePath().toString();
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
                assertTrue(FileUtils.deleteDirectory(unzippedJstl));
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
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
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
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
        try (InputStream is = url.openConnection().getInputStream();) {
            fail("URL '" + path + "' is still deployed");
        } catch (IOException e) {
            assertTrue("success case", true);
            return;
        }
    }

    private Path explode(Path packed) throws IOException {
        return FileUtils.unpackToDir(packed, Paths.get("target/tmp"));
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
        Path defaultContextXml = Paths.get("target/config/context.xml");
        createFileWithContent(defaultContextXml, "<Context crossContext=\"true\"/>");

        // Copy default context.xml.default
        Path defaultHostContextXml = Paths.get("target/config/Catalina/localhost/context.xml.default");
        String content = "<Context>"
            + "<Resource name=\"mail/Session1\" auth=\"Container\" type=\"javax.mail.Session\" mail.smtp.host=\"localhost\"/>" + "</Context>";
        createFileWithContent(defaultHostContextXml, content);

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

            // tests WEB-INF/classes/META-INF/resources
            validateURL("http://localhost:8080/war-with-context-xml-custom-classloader/test.jsp");

            // tests JNDI resources
            // tests cross context functionality
            validateURL("http://localhost:8080/war-with-context-xml-cross-context/index.jsp");
            validateURL("http://localhost:8080/war-with-context-xml-cross-context/forward.jsp");
        } finally {
            this.container.stopWebApplication(handle1);
            bundle1.uninstall();

            this.container.stopWebApplication(handle2);
            bundle2.uninstall();

            Files.delete(defaultContextXml);
            Files.delete(defaultHostContextXml);
        }
    }

    @Test
    public void testInstallWebAppDir() throws Exception {
        // Create web app dir
        Path webAppDir = Paths.get("target/test-classes/simple-web-app-dir");
        Path indexJsp = webAppDir.resolve("index.jsp");
        createFileWithContent(indexJsp, "Hello World!\n"
            + "config.getServletContext().getResourcePaths(/): <%=config.getServletContext().getResourcePaths(\"/\")%>\n"
            + "config.getServletContext().getRealPath(/): <%=config.getServletContext().getRealPath(\"/\")%>\n"
            + "**************  REAL PATH: <%=request.getRealPath(\".\")%>\n"
            + "**************  REAL PATH: <%=request.getRealPath(\"META-INF/.\")%>\n");
        Path metaInf = webAppDir.resolve("META-INF");
        Path manifest = metaInf.resolve("MANIFEST.MF");
        createFileWithContent(manifest, "");
        Path otherMetaInf = webAppDir.resolve("blah/META-INF.");
        Path otherManifest = otherMetaInf.resolve("MANIFEST.MF");
        createFileWithContent(otherManifest, "Manifest-Version: 1.0");
        Path otherDirectory = webAppDir.resolve("blah/META-INF.blah");
        Path otherStaticResource = otherDirectory.resolve("test.txt");
        createFileWithContent(otherStaticResource, "TEST");

        Object[] result = startWebApplicationWith(LOCATION_PREFIX + webAppDir.toAbsolutePath().toString() + "?Web-ContextPath=/simple-web-app-dir",
            "/simple-web-app-dir");

        try {
            validateURL("http://localhost:8080/simple-web-app-dir/index.jsp");
            validateNotFound("http://localhost:8080/simple-web-app-dir/META-INF./MANIFEST.MF");
            validateURLExpectedContent("http://localhost:8080/simple-web-app-dir/blah/META-INF./MANIFEST.MF", "Manifest-Version: 1.0");
            validateURLExpectedContent("http://localhost:8080/simple-web-app-dir/blah/META-INF.blah/test.txt", "TEST");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
            assertTrue(FileUtils.deleteDirectory(webAppDir));
            assertTrue(FileUtils.deleteDirectory(Paths.get("temp")));
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
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=541285
            //validateURLExpectedContent("http://localhost:8080/war-with-resource-references/Bug52974Servlet", new String[] {
            //    "@Resource injection - field: resource", "@Resource injection - method: resource1" });
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
        // In this custom default web.xml the directory listing is enabled
        // Thus we will ensure that a custom default web.xml is used
        Path defaultWebXml = Paths.get("target/config/web.xml");
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
            validateURLExpectedContent("http://localhost:8080/war-with-tld", new String[] { "test.jsp", "0.3 kb" });
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();

            FileUtils.copy(Paths.get("src/test/resources/web.xml"), defaultWebXml);
        }
    }

    @Test
    public void testDirectoryListing() throws Exception {
        // In this custom default web.xml the directory listing is enabled
        // Thus we will ensure that a custom default web.xml is used
        Path defaultWebXml = Paths.get("target/config/web.xml");
        createFileWithContent(defaultWebXml, "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
            + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"\nversion=\"2.5\">\n"
            + "<servlet>\n<servlet-name>default</servlet-name>\n<servlet-class>org.apache.catalina.servlets.DefaultServlet</servlet-class>\n"
            + "<init-param><param-name>debug</param-name><param-value>0</param-value></init-param>\n"
            + "<init-param><param-name>listings</param-name><param-value>true</param-value></init-param>\n"
            + "<load-on-startup>1</load-on-startup>\n</servlet>\n"
            + "<servlet-mapping><servlet-name>default</servlet-name><url-pattern>/</url-pattern></servlet-mapping></web-app>");

        // Create web app dir
        Path webAppDir = Paths.get("target/test-classes/simple-web-app-dir");
        Path testFolder = webAppDir.resolve("test");
        Path testHtml = testFolder.resolve("test.html");
        createFileWithContent(testHtml, "Hello World!");
        Path metaInf = webAppDir.resolve("META-INF");
        Path manifest = metaInf.resolve("MANIFEST.MF");
        createFileWithContent(manifest, "Manifest-Version: 1.0\n" + "Bundle-ManifestVersion: 2\n" + "Web-ContextPath: /simple-web-app-dir\n"
            + "Bundle-SymbolicName: simple-web-app-dir\n\n");

        Object[] result = startWebApplicationWith("reference:file:" + webAppDir.toAbsolutePath().toString(), "/simple-web-app-dir");
        try {
            validateURL("http://localhost:8080/simple-web-app-dir/test/test.html");
            validateURL("http://localhost:8080/simple-web-app-dir/test");

            assertTrue(FileUtils.deleteDirectory(testFolder));
            // there is cacheTTL property which default value is 5s
            Thread.sleep(5000);
            validateNotFound("http://localhost:8080/simple-web-app-dir/test");
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();

            assertTrue(FileUtils.deleteDirectory(webAppDir));
            FileUtils.copy(Paths.get("src/test/resources/web.xml"), defaultWebXml);
        }
    }

    /**
     * Test case for https://bugs.eclipse.org/bugs/show_bug.cgi?id=463163
     */
    @Test
    public void testServerEndpointInOrderedWebFragment() throws Exception {
        Object[] result = startWebApplicationWith(LOCATION_PREFIX + LOCATION_WEBSOCKET, "/websocket");

        try {
            validateURL("http://localhost:8080/websocket/index.html");

            try (Session wsSession = connectToServer("ws://localhost:8080/websocket/endpoint");) {
                final CountDownLatch latch = new CountDownLatch(1);
                final String expectedMessage = "Return: text";
                wsSession.addMessageHandler(new MessageHandler.Whole<String>() {

                    @Override
                    public void onMessage(String message) {
                        System.out.println("Message received: " + message);
                        assertEquals(expectedMessage, message);
                        latch.countDown();
                    }

                });
                wsSession.getBasicRemote().sendText("text");
                boolean latchResult = latch.await(10, TimeUnit.SECONDS);
                assertTrue(latchResult);
            }
        } finally {
            this.container.stopWebApplication((WebApplicationHandle) result[1]);
            ((Bundle) result[0]).uninstall();
        }
    }

    private Session connectToServer(String uri) throws Exception {
        WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
        Session wsSession = wsContainer.connectToServer(new Endpoint() {

            @Override
            public void onError(Session session, Throwable t) {
                System.err.println("onError: " + t);
            }

            @Override
            public void onOpen(Session session, EndpointConfig config) {
                session.getUserProperties().put("endpoint", this);
            }

        }, ClientEndpointConfig.Builder.create().build(), new URI(uri));
        return wsSession;
    }

    private void createFileWithContent(Path file, String content) throws Exception {
        if (Files.notExists(file.getParent())) {
            Files.createDirectories(file.getParent());
        }
        try (BufferedWriter bWriter = Files.newBufferedWriter(file, StandardCharsets.UTF_8);) {
            bWriter.write(content);
            bWriter.flush();
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
