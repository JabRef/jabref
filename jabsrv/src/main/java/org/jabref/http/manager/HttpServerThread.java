package org.jabref.http.manager;

import java.net.URI;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.Server;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.server.RemoteMessageHandler;

import jakarta.ws.rs.ProcessingException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thread wrapper is required to be able to interrupt the http server, e.g. when JabRef is closing down the http server should shutdown as well.
 */
public class HttpServerThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerThread.class);

    private final Server server;
    private final SrvStateManager srvStateManager;
    private final URI uri;
    private final org.jabref.logic.remote.server.RemoteMessageHandler remoteMessageHandler;

    private HttpServer httpServer;

    public HttpServerThread(CliPreferences cliPreferences, SrvStateManager srvStateManager, URI uri, RemoteMessageHandler remoteMessageHandler) {
        this.srvStateManager = srvStateManager;
        this.uri = uri;
        this.remoteMessageHandler = remoteMessageHandler;
        this.server = new Server(cliPreferences);
        this.setName("JabSrv - JabRef HTTP Server on " + uri.getHost() + ":" + uri.getPort());
    }

    @Override
    public void run() {
        try {
            httpServer = this.server.run(srvStateManager, uri, remoteMessageHandler);
        } catch (ProcessingException e) {
            LOGGER.error("Failed to start HTTP server thread", e);
        }
    }

    @Override
    public void interrupt() {
        LOGGER.debug("Interrupting {}", this.getName());
        if (this.httpServer == null) {
            LOGGER.warn("HttpServer is null, cannot shutdown.");
        } else {
            this.httpServer.shutdownNow();
        }
        super.interrupt();
    }

    public boolean started() {
        return httpServer != null && httpServer.isStarted();
    }
}
