package org.jabref.http.manager;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

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

    public synchronized void start(List<Path> filesToServe, URI uri) {
        if (!isOpen()) {
            httpServerThread = new HttpServerThread(filesToServe, uri);
            httpServerThread.start();
        }
    }

    public synchronized void stop() {
        if (isOpen()) {
            httpServerThread.interrupt();
            httpServerThread = null;
        }
    }

    public boolean isOpen() {
        return httpServerThread != null;
    }

    @Override
    public void close() {
        stop();
    }
}
