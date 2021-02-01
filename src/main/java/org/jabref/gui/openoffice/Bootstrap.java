// -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

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

import java.io.BufferedReader;
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

import com.sun.star.bridge.UnoUrlResolver;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.comp.helper.ComponentContext;
import com.sun.star.comp.helper.ComponentContextEntry;
import com.sun.star.comp.loader.JavaLoader;
import com.sun.star.comp.servicemanager.ServiceManager;
import com.sun.star.container.XSet;
import com.sun.star.lang.XInitialization;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lib.util.NativeLibraryLoader;
import com.sun.star.loader.XImplementationLoader;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/** Bootstrap offers functionality to obtain a context or simply
    a service manager.
    The service manager can create a few basic services, whose implementations  are:
    <ul>
    <li>com.sun.star.comp.loader.JavaLoader</li>
    <li>com.sun.star.comp.urlresolver.UrlResolver</li>
    <li>com.sun.star.comp.bridgefactory.BridgeFactory</li>
    <li>com.sun.star.comp.connections.Connector</li>
    <li>com.sun.star.comp.connections.Acceptor</li>
    <li>com.sun.star.comp.servicemanager.ServiceManager</li>
    </ul>

    Other services can be inserted into the service manager by
    using its XSet interface:
    <pre>
        XSet xSet = UnoRuntime.queryInterface( XSet.class, aMultiComponentFactory );
        // insert the service manager
        xSet.insert( aSingleComponentFactory );
    </pre>
*/
public class Bootstrap {

    private static final Random RANDOM_PIPE_NAME = new Random();
    private static boolean M_LOADED_JUH = false;

    private static void insertBasicFactories(XSet xSet, XImplementationLoader xImpLoader) throws Exception {
        // insert the factory of the loader
        xSet.insert(xImpLoader.activate("com.sun.star.comp.loader.JavaLoader", null, null, null));

        // insert the factory of the URLResolver
        xSet.insert(xImpLoader.activate("com.sun.star.comp.urlresolver.UrlResolver", null, null, null));

        // insert the bridgefactory
        xSet.insert(xImpLoader.activate("com.sun.star.comp.bridgefactory.BridgeFactory", null, null, null));

        // insert the connector
        xSet.insert(xImpLoader.activate("com.sun.star.comp.connections.Connector", null, null, null));

        // insert the acceptor
        xSet.insert(xImpLoader.activate("com.sun.star.comp.connections.Acceptor", null, null, null));
    }

    /**
     * Returns an array of default commandline options to start bootstrapped
     * instance of soffice with. You may use it in connection with bootstrap
     * method for example like this:
     * <pre>
     *     List list = Arrays.asList( Bootstrap.getDefaultOptions() );
     *     list.remove("--nologo");
     *     list.remove("--nodefault");
     *     list.add("--invisible");
     *
     *     Bootstrap.bootstrap( list.toArray( new String[list.size()] );
     * </pre>
     *
     * @return an array of default commandline options
     * @see #bootstrap(String[])
     * @since LibreOffice 5.1
     */
    public static final String[] getDefaultOptions() {
        return new String[] {"--nologo", "--nodefault", "--norestore", "--nolockcheck"};
    }

    /**
     * backwards compatibility stub.
     *
     * @param context_entries the hash table contains mappings of entry names (type string) to context entries (type class ComponentContextEntry).
     * @return a new context.
     * @throws Exception if things go awry.
     */
    public static XComponentContext createInitialComponentContext(Hashtable<String, Object> context_entries) throws Exception {
        return createInitialComponentContext((Map<String, Object>) context_entries);
    }

    /**
     * Bootstraps an initial component context with service manager and basic jurt components inserted.
     *
     * @param context_entries the hash table contains mappings of entry names (type string) to context entries (type class ComponentContextEntry).
     * @return a new context.
     * @throws Exception if things go awry.
     */
    public static XComponentContext createInitialComponentContext(Map<String, Object> context_entries) throws Exception {
        ServiceManager xSMgr = new ServiceManager();

        XImplementationLoader xImpLoader = UnoRuntime.queryInterface(XImplementationLoader.class, new JavaLoader());
        XInitialization xInit = UnoRuntime.queryInterface(XInitialization.class, xImpLoader);
        Object[] args = new Object[] {xSMgr};
        xInit.initialize(args);

        // initial component context
        if (context_entries == null) {
            context_entries = new HashMap<>(1);
        }
        // add smgr
        context_entries.put("/singletons/com.sun.star.lang.theServiceManager", new ComponentContextEntry(null, xSMgr));
        // ... xxx todo: add standard entries
        XComponentContext xContext = new ComponentContext(context_entries, null);

        xSMgr.setDefaultContext(xContext);

        XSet xSet = UnoRuntime.queryInterface(XSet.class, xSMgr);
        // insert basic jurt factories
        insertBasicFactories(xSet, xImpLoader);

        return xContext;
    }

    /**
     * Bootstraps a servicemanager with the jurt base components registered.
     * <p>
     * See also UNOIDL <code>com.sun.star.lang.ServiceManager</code>.
     *
     * @return a freshly bootstrapped service manager
     * @throws Exception if things go awry.
     */
    public static XMultiServiceFactory createSimpleServiceManager() throws Exception {
        return UnoRuntime.queryInterface(XMultiServiceFactory.class, createInitialComponentContext((Map<String, Object>) null).getServiceManager());
    }

    /**
     * Bootstraps the initial component context from a native UNO installation.
     *
     * @return a freshly bootstrapped component context.
     * <p>
     * See also
     * <code>cppuhelper/defaultBootstrap_InitialComponentContext()</code>.
     * @throws Exception if things go awry.
     */
    public static final XComponentContext defaultBootstrap_InitialComponentContext() throws Exception {
        return defaultBootstrap_InitialComponentContext((String) null, (Map<String, String>) null);
    }

    /**
     * Backwards compatibility stub.
     *
     * @param ini_file             ini_file (may be null: uno.rc besides cppuhelper lib)
     * @param bootstrap_parameters bootstrap parameters (maybe null)
     * @return a freshly bootstrapped component context.
     * @throws Exception if things go awry.
     */
    public static final XComponentContext defaultBootstrap_InitialComponentContext(String ini_file, Hashtable<String, String> bootstrap_parameters) throws Exception {
        return defaultBootstrap_InitialComponentContext(ini_file, (Map<String, String>) bootstrap_parameters);
    }

    /**
     * Bootstraps the initial component context from a native UNO installation.
     * <p>
     * See also
     * <code>cppuhelper/defaultBootstrap_InitialComponentContext()</code>.
     *
     * @param ini_file             ini_file (may be null: uno.rc besides cppuhelper lib)
     * @param bootstrap_parameters bootstrap parameters (maybe null)
     * @return a freshly bootstrapped component context.
     * @throws Exception if things go awry.
     */
    public static final XComponentContext defaultBootstrap_InitialComponentContext(String ini_file, Map<String, String> bootstrap_parameters) throws Exception {
        // jni convenience: easier to iterate over array than calling Hashtable
        String pairs[] = null;
        if (null != bootstrap_parameters) {
            pairs = new String[2 * bootstrap_parameters.size()];
            int n = 0;
            for (Map.Entry<String, String> bootstrap_parameter : bootstrap_parameters.entrySet()) {
                pairs[n++] = bootstrap_parameter.getKey();
                pairs[n++] = bootstrap_parameter.getValue();
            }
        }

        if (!M_LOADED_JUH) {
            if ("The Android Project".equals(System.getProperty("java.vendor"))) {
                // Find out if we are configured with DISABLE_DYNLOADING or
                // not. Try to load the lo-bootstrap shared library which
                // won't exist in the DISABLE_DYNLOADING case. (And which will
                // be already loaded otherwise, so nothing unexpected happens
                // that case.) Yeah, this would be simpler if I just could be
                // bothered to keep a separate branch for DISABLE_DYNLOADING
                // on Android, merging in master periodically, until I know
                // for sure whether it is what I want, or not.

                boolean disable_dynloading = false;
                try {
                    System.loadLibrary("lo-bootstrap");
                } catch (UnsatisfiedLinkError e) {
                    disable_dynloading = true;
                }

                if (!disable_dynloading) {
                    NativeLibraryLoader.loadLibrary(Bootstrap.class.getClassLoader(), "juh");
                }
            } else {
                NativeLibraryLoader.loadLibrary(Bootstrap.class.getClassLoader(), "juh");
            }
            M_LOADED_JUH = true;
        }
        return UnoRuntime.queryInterface(XComponentContext.class, cppuhelper_bootstrap(ini_file, pairs, Bootstrap.class.getClassLoader()));
    }

    private static native Object cppuhelper_bootstrap(String ini_file, String bootstrap_parameters[], ClassLoader loader) throws Exception;

    /**
     * Bootstraps the component context from a UNO installation.
     *
     * @return a bootstrapped component context.
     * @throws BootstrapException if things go awry.
     * @since UDK 3.1.0
     */
    public static final XComponentContext bootstrap(Path ooPath) throws BootstrapException {
        String[] defaultArgArray = getDefaultOptions();
        return bootstrap(defaultArgArray, ooPath);
    }

    /**
     * Bootstraps the component context from a UNO installation.
     *
     * @param argArray an array of strings - commandline options to start instance of soffice with
     * @return a bootstrapped component context.
     * @throws BootstrapException if things go awry.
     * @see #getDefaultOptions()
     * @since LibreOffice 5.1
     */
    public static final XComponentContext bootstrap(String[] argArray, Path path) throws BootstrapException {

        XComponentContext xContext = null;

        try {
            // create default local component context
            XComponentContext xLocalContext = createInitialComponentContext((Map<String, Object>) null);
            if (xLocalContext == null) {
                throw new BootstrapException("no local component context!");
            }

            // create call with arguments
            // We need a socket, pipe does not work. https://api.libreoffice.org/examples/examples.html
            String[] cmdArray = new String[argArray.length + 2];
            cmdArray[0] = path.toAbsolutePath().toString();
            cmdArray[1] = ("--accept=socket,host=localhost,port=2083" + ";urp;");

            System.arraycopy(argArray, 0, cmdArray, 2, argArray.length);

            // start office process
            Process p = Runtime.getRuntime().exec(cmdArray);
            pipe(p.getInputStream(), System.out, "CO> ");
            pipe(p.getErrorStream(), System.err, "CE> ");

            // initial service manager
            XMultiComponentFactory xLocalServiceManager = xLocalContext.getServiceManager();
            if (xLocalServiceManager == null) {
                throw new BootstrapException("no initial service manager!");
            }

            // create a URL resolver
            XUnoUrlResolver xUrlResolver = UnoUrlResolver.create(xLocalContext);

            // connection string
            String sConnect = "uno:socket,host=localhost,port=2083" + ";urp;StarOffice.ComponentContext";

            // wait until office is started
            for (int i = 0; ; ++i) {
                try {
                    // try to connect to office
                    Object context = xUrlResolver.resolve(sConnect);
                    xContext = UnoRuntime.queryInterface(XComponentContext.class, context);
                    if (xContext == null) {
                        throw new BootstrapException("no component context!");
                    }
                    break;
                } catch (com.sun.star.connection.NoConnectException ex) {
                    // Wait 500 ms, then try to connect again, but do not wait
                    // longer than 5 min (= 600 * 500 ms) total:
                    if (i == 600) {
                        throw new BootstrapException(ex);
                    }
                    Thread.sleep(500);
                }
            }
        } catch (BootstrapException e) {
            throw e;
        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new BootstrapException(e);
        }

        return xContext;
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
                    e.printStackTrace(System.err);
                } catch (java.io.IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        }.start();
    }
}

// vim:set shiftwidth=4 softtabstop=4 expandtab:
