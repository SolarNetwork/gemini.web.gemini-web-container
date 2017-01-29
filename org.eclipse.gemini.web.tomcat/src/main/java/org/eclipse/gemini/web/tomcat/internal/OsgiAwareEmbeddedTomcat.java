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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.naming.java.javaURLContextFactory;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.eclipse.gemini.web.core.spi.ServletContainerException;
import org.eclipse.gemini.web.tomcat.internal.loader.ChainedClassLoader;
import org.eclipse.gemini.web.tomcat.internal.support.BundleFileResolverFactory;
import org.eclipse.gemini.web.tomcat.internal.support.PackageAdminBundleDependencyDeterminer;
import org.eclipse.virgo.util.osgi.ServiceRegistrationTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public final class OsgiAwareEmbeddedTomcat extends Tomcat {

    private static final String USER_DIR = "user.dir";

    private static final String ROOT_CONTEXT_PATH = "";

    private static final String ROOT_PATH = "/";

    static final String USE_NAMING = "useNaming";

    static final String TOMCAT_NAMING_ENABLED = "tomcat";

    static final String OSGI_NAMING_ENABLED = "osgi";

    static final String NAMING_DISABLED = "disabled";

    static final String CATALINA_USE_NAMING = "catalina.useNaming";

    private static final String JNDI_URLSCHEME = "osgi.jndi.url.scheme";

    private static final String JAVA_JNDI_URLSCHEME = "java";

    private final static Logger LOGGER = LoggerFactory.getLogger(OsgiAwareEmbeddedTomcat.class);

    private final ExtendCatalina catalina = new ExtendCatalina();

    private final BundleContext bundleContext;

    private Path configDir;

    private String defaultContextXml;

    private String defaultWeb;

    private String oldCatalinaBaseDir;

    private String oldCatalinaHomeDir;

    /**
     * Custom mappings of login methods to authenticators
     */
    private volatile HashMap<String, Authenticator> authenticators;

    private final Object monitor = new Object();

    private final ServiceRegistrationTracker tracker = new ServiceRegistrationTracker();

    private final DelegatingJarScannerCustomizer jarScannerCustomizer;

    private final JarScanner bundleDependenciesJarScanner;

    private final JarScanner defaultJarScanner;

    private String hostConfigDir;

    OsgiAwareEmbeddedTomcat(BundleContext context, ServiceTracker<?, ?> urlConverterTracker) {
        this.bundleContext = context;
        this.bundleDependenciesJarScanner = new BundleDependenciesJarScanner(new PackageAdminBundleDependencyDeterminer(),
            BundleFileResolverFactory.createBundleFileResolver(), context, urlConverterTracker);
        this.defaultJarScanner = new StandardJarScanner();
        this.jarScannerCustomizer = new DelegatingJarScannerCustomizer(context);
    }

    /**
     * Start the server.
     *
     * @throws LifecycleException
     */
    @Override
    public void start() throws LifecycleException {
        this.jarScannerCustomizer.open();
        getServer();
        this.server.start();
    }

    @Override
    public void stop() throws LifecycleException {
        super.stop();
        this.jarScannerCustomizer.close();
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public Service getService() {
        Server server = getServer();
        Service[] findServices = server.findServices();
        if (findServices != null && findServices.length > 0) {
            return findServices[0];
        }
        throw new IllegalStateException("Unable to locate Service.");
    }

    @Override
    public Host getHost() {
        return findHost();
    }

    @Override
    public Engine getEngine() {
        return findEngine();
    }

    private Engine findEngine() {
        Server server = getServer();
        Service[] findServices = server.findServices();
        for (Service service : findServices) {
            Engine container = service.getContainer();
            if (container != null) {
                return container;
            }
        }
        throw new IllegalStateException("Unable to locate Engine.");
    }

    private Host findHost() {
        Engine engine = findEngine();
        Container[] children = engine.findChildren();
        for (Container container : children) {
            if (container instanceof Host) {
                return (Host) container;
            }
        }
        throw new IllegalStateException("Unable to locate Host.");
    }

    @Override
    public void init() throws LifecycleException {
        getServer();

        initNaming();

        this.server.init();
    }

    private void initNaming() {
        String useNaming = this.bundleContext.getProperty(USE_NAMING);
        if (useNaming == null) {
            useNaming = System.getProperty(USE_NAMING);
        }
        if (useNaming == null) {
            useNaming = TOMCAT_NAMING_ENABLED;
        }

        if (NAMING_DISABLED.equals(useNaming)) {
            System.setProperty(CATALINA_USE_NAMING, Boolean.FALSE.toString());
        } else {
            enableNaming(useNaming);
        }
    }

    private void enableNaming(String useNaming) {
        super.enableNaming();

        if (OSGI_NAMING_ENABLED.equals(useNaming)) {
            registerInitialContextFactory();
            registerJavaURLContextFactory();
            registerObjectFactoryBuilder();
        }
    }

    @Override
    public void destroy() throws LifecycleException {
        super.destroy();

        this.tracker.unregisterAll();

        if (this.oldCatalinaBaseDir != null) {
            System.setProperty(Globals.CATALINA_BASE_PROP, this.oldCatalinaBaseDir);
        }

        if (this.oldCatalinaHomeDir != null) {
            System.setProperty(Globals.CATALINA_HOME_PROP, this.oldCatalinaHomeDir);
        }
    }

    @Override
    public Context addWebapp(String path, String docBase) {
        return addWebapp(path, docBase, null);
    }

    Context addWebapp(String path, String docBase, Bundle bundle) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating context [" + path + "] with docBase [" + docBase + "].");
        }

        StandardContext context = new ExtendedStandardContext(bundle);

        ContextConfig config = new ExtendedContextConfig();

        if (this.configDir == null) {
            // Allocate the tomcat's configuration directory
            this.configDir = TomcatConfigLocator.resolveConfigDir(this.bundleContext);
        }

        if (this.defaultWeb == null) {
            // Allocate the default web.xml
            this.defaultWeb = WebappConfigLocator.resolveDefaultWebXml(this.configDir);
        }
        config.setDefaultWebXml(this.defaultWeb);

        // Allocate default context.xml
        if (this.defaultContextXml == null) {
            this.defaultContextXml = WebappConfigLocator.resolveDefaultContextXml(this.configDir);
        }
        context.setDefaultContextXml(this.defaultContextXml);

        Host host = getHost();
        if (this.hostConfigDir == null) {
            this.hostConfigDir = TomcatConfigLocator.resolveHostConfigDir(this.configDir, host);
        }
        if (this.hostConfigDir != null) {
            host.setXmlBase(this.hostConfigDir);
            host.getConfigBaseFile();
        }

        // Allocate the web application's configuration directory
        Path configLocation = WebappConfigLocator.resolveWebappConfigDir(this.configDir, host);

        // If web application's context.xml is existing, set it to the StandardContext
        try {
            context.setConfigFile(WebappConfigLocator.resolveWebappContextXml(path, docBase, configLocation, bundle));
        } catch (MalformedURLException e) {
            throw new ServletContainerException("Cannot resolve web application's context.xml [" + docBase + "].", e);
        }

        context.setDocBase(docBase);
        context.setPath(path.equals(ROOT_PATH) ? ROOT_CONTEXT_PATH : path);
        context.setName(context.getPath());

        context.setJarScanner(getJarScanner(bundle));

        context.setParent(host);

        config.setCustomAuthenticators(this.authenticators);
        ((Lifecycle) context).addLifecycleListener(config);

        return context;
    }

    void configure(InputStream configuration) {
        Digester digester = this.catalina.createStartDigester();
        digester.push(this);

        ClassLoader[] loaders = new ClassLoader[] { Catalina.class.getClassLoader(), getClass().getClassLoader() };
        ChainedClassLoader chainedClassLoader = ChainedClassLoader.create(loaders);
        chainedClassLoader.setBundle(this.bundleContext.getBundle());
        digester.setClassLoader(chainedClassLoader);

        try {
            digester.parse(configuration);
        } catch (IOException e) {
            throw new ServletContainerException("Error reading Tomcat configuration file.", e);
        } catch (SAXException e) {
            throw new ServletContainerException("Error parsing Tomcat XML configuration.", e);
        }

        initBaseDir();

        // Allocate the tomcat's configuration directory
        this.configDir = TomcatConfigLocator.resolveConfigDir(this.bundleContext);

        // Allocate default context.xml
        this.defaultContextXml = WebappConfigLocator.resolveDefaultContextXml(this.configDir);

        // Allocate the default web.xml
        this.defaultWeb = WebappConfigLocator.resolveDefaultWebXml(this.configDir);

        Host host = getHost();
        this.hostConfigDir = TomcatConfigLocator.resolveHostConfigDir(this.configDir, host);
        if (this.hostConfigDir != null) {
            host.setXmlBase(this.hostConfigDir);
            host.getConfigBaseFile();
        }
    }

    @Override
    protected void initBaseDir() {
        String catalinaHome = System.getProperty(Globals.CATALINA_HOME_PROP);
        if (this.basedir == null) {
            this.basedir = System.getProperty(Globals.CATALINA_BASE_PROP);
        }
        if (this.basedir == null) {
            this.basedir = catalinaHome;
        }
        if (this.basedir == null) {
            // Create a temp dir.
            this.basedir = System.getProperty(USER_DIR) + "/tomcat." + this.port;
        }
        Path baseFile = Paths.get(this.basedir);
        try {
            Files.createDirectories(baseFile);
        } catch (IOException e1) {
            throw new IllegalStateException("Cannot create directories for [" + baseFile + "].", e1);
        }
        try {
            this.basedir = baseFile.toRealPath().toString();
        } catch (IOException e) {
            this.basedir = baseFile.toAbsolutePath().toString();
        }

        this.server.setCatalinaBase(new File(this.basedir));
        this.oldCatalinaBaseDir = System.setProperty(Globals.CATALINA_BASE_PROP, this.basedir);

        if (catalinaHome == null) {
            this.server.setCatalinaHome(new File(this.basedir));
        } else {
            Path homeFile = Paths.get(catalinaHome);
            try {
                Files.createDirectories(homeFile);
            } catch (IOException e1) {
                throw new IllegalStateException("Cannot create directories for [" + homeFile + "].", e1);
            }
            try {
                this.server.setCatalinaHome(homeFile.toRealPath().toFile());
            } catch (IOException e) {
                this.server.setCatalinaHome(homeFile.toAbsolutePath().toFile());
            }
        }

        this.oldCatalinaHomeDir = System.setProperty(Globals.CATALINA_HOME_PROP, this.basedir);
    }

    /**
     * Maps the specified login method to the specified authenticator, allowing the mappings in
     * org/apache/catalina/startup/Authenticators.properties to be overridden.
     *
     * @param authenticator Authenticator to handle authentication for the specified login method
     * @param loginMethod Login method that maps to the specified authenticator
     *
     * @throws IllegalArgumentException if the specified authenticator does not implement the org.apache.catalina.Valve
     *         interface
     */
    void addAuthenticator(Authenticator authenticator, String loginMethod) {
        if (!(authenticator instanceof Valve)) {
            throw new IllegalArgumentException("Specified Authenticator is not a Valve");
        }
        if (this.authenticators == null) {
            synchronized (this.monitor) {
                if (this.authenticators == null) {
                    this.authenticators = new HashMap<>();
                }
            }
        }
        this.authenticators.put(loginMethod, authenticator);
    }

    /**
     * Overrides {@link Catalina} to provide public access to {@link Catalina#createStartDigester}.
     *
     *
     */
    private static class ExtendCatalina extends Catalina {

        @Override
        public Digester createStartDigester() {
            return super.createStartDigester();
        }

    }

    /**
     * Override {@link ContextConfig}. This changes the {@link ClassLoader} used to load the web-embed.xml to the
     * <code>ClassLoader</code> of this bundle.
     *
     *
     */
    private static class ExtendedContextConfig extends ContextConfig {
    }

    /**
     * Registers the <code>ObjectFactoryBuilder</code> implementation that is responsible for loading Tomcat's Object
     * Factories.
     */
    private void registerObjectFactoryBuilder() {
        ServiceRegistration<ObjectFactoryBuilder> serviceRegistration = this.bundleContext.registerService(ObjectFactoryBuilder.class,
            new ObjectFactoryBuilder() {

                @Override
                public ObjectFactory createObjectFactory(Object obj, Hashtable<?, ?> environment) throws NamingException {
                    if (obj instanceof Reference) {
                        final Reference reference = (Reference) obj;
                        final String factory = reference.getFactoryClassName();
                        if (factory != null) {
                            try {
                                Class<?> clazz = getClass().getClassLoader().loadClass(factory);
                                return (ObjectFactory) clazz.newInstance();
                            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info("Error while trying to create object factory [" + factory + "]", e);
                                }
                            }
                        }
                    }
                    return null;
                }

            }, null);
        this.tracker.track(serviceRegistration);
    }

    /**
     * Registers <code>org.apache.naming.java.javaURLContextFactory</code> as URL Context Factory for 'java' URL scheme.
     * In the traditional way this factory is specified via <code>java.naming.factory.initial</code> system property.
     */
    private void registerJavaURLContextFactory() {
        Dictionary<String, String> serviceProperties = new Hashtable<>();
        serviceProperties.put(JNDI_URLSCHEME, JAVA_JNDI_URLSCHEME);
        ServiceRegistration<ObjectFactory> serviceRegistration = this.bundleContext.registerService(ObjectFactory.class, new javaURLContextFactory(),
            serviceProperties);
        this.tracker.track(serviceRegistration);
    }

    /**
     * Registers the <code>InitialContextFactory</code> implementation that is responsible for loading
     * <code>org.apache.naming.java.javaURLContextFactory</code>. In the traditional way this factory is specified via
     * <code>java.naming.factory.initial</code> system property.
     */
    private void registerInitialContextFactory() {
        ServiceRegistration<?> serviceRegistration = this.bundleContext.registerService(new String[] { InitialContextFactory.class.getName(),
            javaURLContextFactory.class.getName() }, new javaURLContextFactory(), null);
        this.tracker.track(serviceRegistration);
    }

    private JarScanner getJarScanner(Bundle bundle) {
        JarScanner[] jarScanners = new JarScanner[] { this.bundleDependenciesJarScanner, this.defaultJarScanner };

        JarScanner[] chainExtensions = this.jarScannerCustomizer.extendJarScannerChain(bundle);

        JarScanner[] finalJarScanners = null;
        if (chainExtensions != null && chainExtensions.length > 0) {
            finalJarScanners = new JarScanner[jarScanners.length + chainExtensions.length];
            System.arraycopy(jarScanners, 0, finalJarScanners, 0, jarScanners.length);
            System.arraycopy(chainExtensions, 0, finalJarScanners, jarScanners.length, chainExtensions.length);
        } else {
            finalJarScanners = jarScanners;
        }

        return new ChainingJarScanner(finalJarScanners);
    }

}
