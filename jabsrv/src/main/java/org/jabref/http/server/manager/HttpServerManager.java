package org.jabref.http.server.manager;

import java.net.URI;

import org.jabref.http.SrvStateManager;
import org.jabref.logic.preferences.CliPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the HttpServerThread through typical life cycle methods.
///
/// open -> start -> stop
/// openAndStart -> stop
///
/// Observer: isOpen, isNotStartedBefore
public class HttpServerManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerManager.class);

    private HttpServerThread httpServerThread;

    public synchronized void start(CliPreferences preferences, SrvStateManager srvStateManager, URI uri) {
        if (httpServerThread != null) {
            LOGGER.warn("HTTP server manager already started, cannot start again.");
            return;
        }

        httpServerThread = new HttpServerThread(preferences, srvStateManager, uri);
        // This enqueues the thread to run in the background
        // The JVM will take care of running it at some point in time in the future
        // Thus, we cannot check directly if it really runs
        httpServerThread.start();
        LOGGER.debug("Triggered HTTP server start up.");
    }

    public synchronized void stop() {
        LOGGER.debug("Stopping HTTP server manager...");
        if (httpServerThread != null) {
            httpServerThread.interrupt();
            httpServerThread = null;
            LOGGER.debug("HTTP server stopped successfully.");
        } else {
            LOGGER.debug("HTTP server is not started, nothing to stop.");
        }
    }

    @Override
    public void close() {
        LOGGER.debug("Closing HTTP server manager...");
        stop();
    }
}
