package org.jabref.http.manager;

import java.net.URI;

import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabaseContext;

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

    public synchronized void start(ObservableList<BibDatabaseContext> contextsToServe, URI uri) {
        if (!isStarted()) {
            httpServerThread = new HttpServerThread(contextsToServe, uri);
            httpServerThread.start();
        }
    }

    public synchronized void stop() {
        LOGGER.debug("Stopping HTTP server manager...");
        if (isStarted()) {
            httpServerThread.interrupt();
            httpServerThread = null;
            LOGGER.debug("HTTP server manager stopped successfully.");
        } else {
            LOGGER.debug("HTTP server manager is not started, nothing to stop.");
        }
    }

    private boolean isStarted() {
        return httpServerThread != null;
    }

    @Override
    public void close() {
        LOGGER.debug("Closing HTTP server manager...");
        stop();
    }
}
