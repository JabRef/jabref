/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This file incorporates work covered by the following license notice:
 *
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements. See the NOTICE file distributed
 *   with this work for additional information regarding copyright
 *   ownership. The ASF licenses this file to you under the Apache
 *   License, Version 2.0 (the "License"); you may not use this file
 *   except in compliance with the License. You may obtain a copy of
 *   the License at http://www.apache.org/licenses/LICENSE-2.0 .
 */

package org.jabref.gui.openoffice;

// ATTENTION: This file is imported from LibreOffice sources and is not part of JabRef
// It has been modified to use SLF4J instead of System.err for logging

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.bridge.UnoUrlResolver;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.comp.helper.ComponentContext;
import com.sun.star.comp.helper.ComponentContextEntry;
import com.sun.star.comp.loader.JavaLoader;
import com.sun.star.connection.ConnectionSetupException;
import com.sun.star.connection.NoConnectException;
import com.sun.star.container.XSet;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.UnoUrl;
import com.sun.star.loader.XImplementationLoader;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * A simple bootstrap mechanism.
 */
public class Bootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    private static final Random RANDOM_PIPE_NAME = new Random();
    private static boolean M_LOADED_JUH = false;

    private static void insertBasicConnectionServiceFactory(XSet xSet, XImplementationLoader xImpLoader,
            String factoryUrl) throws Exception {
        // insert the factory
        XSingleComponentFactory xFactory = xImpLoader.activate("com.sun.star.comp.connections.Connector",
                factoryUrl, "com.sun.star.comp.connections.Acceptor", null);

        xSet.insert(xFactory.createInstanceWithContext(null));
    }

    /**
     * Creates a named pipe for bootstrapping a connection.
     *
     * @return the name of the created pipe.
     */
    public static String getPipeName() {
        // create a unique pipe name
        String aPipeName = "uno" + Long.toString(RANDOM_PIPE_NAME.nextLong() & 0x7fffffffffffffffL);
        return aPipeName;
    }

    /**
     * Bootstraps a servicemanager with a jurt.
     * <p>
     * Only for internal use.
     */
    private static XComponentContext createInitialComponentContext(Map<String, Object> context_entries)
            throws Exception {
        // get the factory
        // for compatibility: use the deprecated url, if it is in the context_entries
        String sOfficeFactoryUrl = (String) context_entries.get("OFFICECONNECTIONFACTORYURL");
        if (sOfficeFactoryUrl == null) {
            sOfficeFactoryUrl = (String) context_entries.get("com.sun.star.office.ConnectionFactoryURL");
        }
        if (sOfficeFactoryUrl == null) {
            throw new BootstrapException("no office resource adapter url given");
        }

        // ensure that we have the juh classloader

        // create default local component context
        XComponentContext xLocalContext = ComponentContext.createComponentContext(
                new HashMap<String, Object>(), null);

        // initial service manager
        XImplementationLoader xImpLoader = null;

        // try the factory
        try {
            // create a resource adapter
            JavaLoader aJavaLoader = new JavaLoader();
            xImpLoader = aJavaLoader;

            // create service manager on the fly
            XRegistryKey xRootKey = null;
            XSingleComponentFactory xLocalFactory = xImpLoader.activate("com.sun.star.comp.servicemanager.ServiceManager",
                    sOfficeFactoryUrl, "com.sun.star.comp.stoc.DLLComponentLoader", xRootKey);

            XMultiComponentFactory xSrvMgr = (XMultiComponentFactory) UnoRuntime.queryInterface(
                    XMultiComponentFactory.class, xLocalFactory.createInstanceWithContext(xLocalContext));

            // Hack!!! Another way is necessary to initialize the service manager
            XSet xSet = (XSet) UnoRuntime.queryInterface(XSet.class, xSrvMgr);

            // get uno url
            String sUnoUrl = (String) context_entries.get("UNO_URL");
            if (sUnoUrl == null) {
                sUnoUrl = (String) context_entries.get("com.sun.star.office.ServiceManagerURL");
            }
            if (sUnoUrl == null) {
                throw new BootstrapException("no uno url given");
            }
            UnoUrl aURL = UnoUrl.parseUnoUrl(sUnoUrl);

            // Insert the service manager
            ComponentContextEntry[] smLcbInstanceArray = new ComponentContextEntry[1];
            smLcbInstanceArray[0] = new ComponentContextEntry("com.sun.star.ServiceManager", xSrvMgr);
            XComponentContext xComponentContext = new ComponentContext(new HashMap<String, Object>(),
                    xLocalContext, smLcbInstanceArray);

            XMultiComponentFactory xLocalServiceManager = xComponentContext.getServiceManager();

            // insert basic uno services
            M_LOADED_JUH = true;
            insertBasicConnectionServiceFactory(xSet, xImpLoader, sOfficeFactoryUrl);

            return xComponentContext;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Bootstraps the component context from a UNO installation.
     *
     * @return a bootstrapped component context.
     * @param sOffice path to UNO installation
     */
    public static XComponentContext bootstrap(Path sOffice) throws BootstrapException {
        XComponentContext xComponentContext = null;

        try {
            HashMap<String, String> context_entries = new HashMap<>();
            // create the pipe for the boostrap connection
            String aPipeName = getPipeName();

            context_entries.put("PIPE_NAME", aPipeName);
            // Build URL
            context_entries.put("UNO_URL", "uno:pipe,name=" + aPipeName + ";urp;StarOffice.ComponentContext");

            context_entries.put("OFFICECONNECTIONFACTORYURL", "jurt:uno_connector.uno_service.com.sun.star.connection.connector");

            XComponentContext xLocalContext = createInitialComponentContext(
                    new HashMap<>(context_entries));

            XMultiComponentFactory xLocalServiceManager = xLocalContext.getServiceManager();

            // get the local UnoUrlResolver service factory
            final Object xUnoUrlResolverFactory = xLocalServiceManager.createInstanceWithContext(
                    "com.sun.star.bridge.UnoUrlResolver", xLocalContext);
            XUnoUrlResolver xUnoUrlResolver = (XUnoUrlResolver) UnoRuntime.queryInterface(
                    XUnoUrlResolver.class, xUnoUrlResolverFactory);

            // --------------------------------------------------------------------------------
            // try to connect to office

            String[] cmdArray = new String[4];
            // create call with arguments
            cmdArray[0] = sOffice.toAbsolutePath() + "\\soffice.exe";

            cmdArray[1] = "--nologo";
            cmdArray[2] = "--nodefault";
            // create the url with the pipe name
            cmdArray[3] = "--accept=pipe,name=" + aPipeName + ";urp;";

            // start office process
            Process p = Runtime.getRuntime().exec(cmdArray);
            pipe(p.getInputStream(), System.out, "CO> ");
            // Using a special LoggerPrintStream to capture error output
            PrintStream loggerPrintStream = new PrintStream(System.err) {
                @Override
                public void println(String x) {
                    LOGGER.error(x);
                }
            };
            pipe(p.getErrorStream(), loggerPrintStream, "CE> ");

            // initial service manager
            XMultiComponentFactory xServiceManager = null;

            // try to get the office component context
            for (int i = 0;; ++i) {
                try {
                    Object initObject = xUnoUrlResolver.resolve(context_entries.get("UNO_URL"));
                    xComponentContext = (XComponentContext) UnoRuntime.queryInterface(
                            XComponentContext.class, initObject);
                    XMultiComponentFactory xFactory = xComponentContext.getServiceManager();
                    if (null != xFactory) {
                        xServiceManager = xFactory;
                        break;
                    }
                } catch (NoConnectException ex) {
                    // Wait 500 milliseconds, then try to connect again, but no
                    // more than 5 seconds (= 10 * 500 milliseconds)
                    if (i >= 10) {
                        throw new BootstrapException("cannot establish a connection", ex);
                    }
                    Thread.sleep(500);
                }
            }

            // just to be safe
            if (null == xComponentContext) {
                throw new BootstrapException();
            }

            // Add an event handler that cares for office termination to kill the
            // temp. bootstrap instance on office shutdown.
            XComponent xComponent = (XComponent) UnoRuntime.queryInterface(
                    XComponent.class, xComponentContext);
            XEventListener xEventListener = new XEventListener() {
                @Override
                public void disposing(EventObject aSourceObj) {
                    try {
                        p.destroy();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            xComponent.addEventListener(xEventListener);

            return xComponentContext;
        } catch (ConnectionSetupException | InterruptedException | IOException e) {
            throw new BootstrapException(e);
        }

        return xComponentContext;
    }

    private static void pipe(final InputStream in, final PrintStream out, final String prefix) {
        new Thread("Pipe: " + prefix) {
            @Override
            public void run() {
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

                    for (; ; ) {
                        String s = r.readLine();
                        if (s == null) {
                            break;
                        }
                        out.println(prefix + s);
                    }
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error("Unsupported encoding", e);
                } catch (IOException e) {
                    LOGGER.error("IO error", e);
                }
            }
        }.start();
    }
}

// vim:set shiftwidth=4 softtabstop=4 expandtab:
