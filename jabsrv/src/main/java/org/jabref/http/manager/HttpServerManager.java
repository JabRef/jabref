package org.jabref.http.manager;

import java.net.URI;

import org.jabref.http.SrvStateManager;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.preferences.CliPreferences;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the HttpServerThread through typical life cycle methods.
///
/// Observer: isOpen, isNotStartedBefore
@NullMarked
public class HttpServerManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerManager.class);

    @Nullable
    private HttpServerThread httpServerThread;

    public synchronized void start(CliPreferences preferences, SrvStateManager srvStateManager, URI uri) {
        start(preferences, srvStateManager, null, uri);
    }

    public synchronized void start(CliPreferences preferences, SrvStateManager srvStateManager, @Nullable UiMessageHandler uiMessageHandler, URI uri) {
        if (httpServerThread != null) {
            LOGGER.warn("HTTP server manager already started, cannot start again.");
            return;
        }

        httpServerThread = new HttpServerThread(preferences, srvStateManager, uiMessageHandler, uri);
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
