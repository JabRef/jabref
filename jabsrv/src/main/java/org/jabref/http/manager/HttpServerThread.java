package org.jabref.http.manager;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.jabref.http.server.Server;

import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thread wrapper is required to be able to interrupt the http server, e.g. when JabRef is closing down the http server should shutdown as well.
 */
public class HttpServerThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerThread.class);

    private final Server server;
    private final List<Path> filesToServe;
    private final URI uri;

    private HttpServer httpServer;

    public HttpServerThread(List<Path> filesToServe, URI uri) {
        this.filesToServe = filesToServe;
        this.uri = uri;
        this.server = new Server();
        this.setName("JabSrv - JabRef HTTP Server on " + uri.getHost() + ":" + uri.getPort());
    }

    @Override
    public void run() {
        httpServer = this.server.run(filesToServe, uri);
    }

    @Override
    public void interrupt() {
        LOGGER.debug("Interrupting {}", this.getName());
        this.httpServer.shutdownNow();
        super.interrupt();
    }
}
