package org.jabref.http.manager;

import java.net.URI;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.Server;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.server.ConnectorTokenManager;

import jakarta.ws.rs.ProcessingException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// This thread wrapper is required to be able to interrupt the http server, e.g. when JabRef is closing down the http server should shutdown as well.
@NullMarked
public class HttpServerThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerThread.class);

    private final Server server;
    private final SrvStateManager srvStateManager;
    private final URI uri;

    @Nullable
    private final UiMessageHandler uiMessageHandler;

    @Nullable
    private final ConnectorTokenManager tokenManager;

    @Nullable
    private HttpServer httpServer;

    /// @param uiMessageHandler - non-null for GUI usage
    /// @param tokenManager - non-null for GUI usage, shared with the preferences UI
    public HttpServerThread(CliPreferences cliPreferences, SrvStateManager srvStateManager, @Nullable UiMessageHandler uiMessageHandler, @Nullable ConnectorTokenManager tokenManager, URI uri) {
        this.srvStateManager = srvStateManager;
        this.uiMessageHandler = uiMessageHandler;
        this.tokenManager = tokenManager;
        this.uri = uri;
        this.server = new Server(cliPreferences);
        this.setName("JabSrv - JabRef HTTP Server on " + uri.getHost() + ":" + uri.getPort());
    }

    @Override
    public void run() {
        try {
            httpServer = this.server.run(srvStateManager, uiMessageHandler, tokenManager, uri);
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
