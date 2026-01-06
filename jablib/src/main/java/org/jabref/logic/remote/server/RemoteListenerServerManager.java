package org.jabref.logic.remote.server;

import java.io.IOException;
import java.net.BindException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages both the RemoteServerThread (TCP) and WebSocketListenerServerThread through typical life cycle methods.
///
/// open -> start -> stop
/// openAndStart -> stop
///
/// Observer: isOpen, isNotStartedBefore
public class RemoteListenerServerManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteListenerServerManager.class);

    private RemoteListenerServerThread remoteServerThread;
    private WebSocketListenerServerThread webSocketServerThread;

    public void stop() {
        LOGGER.debug("Stopping RemoteListenerServerManager");
        if (isOpen()) {
            remoteServerThread.interrupt();
            remoteServerThread = null;
            LOGGER.debug("RemoteListenerServerManager stopped successfully.");
        } else {
            LOGGER.debug("RemoteListenerServerManager was not open, nothing to stop.");
        }

        if (isWebSocketOpen()) {
            webSocketServerThread.interrupt();
            webSocketServerThread = null;
            LOGGER.debug("WebSocketListenerServerManager stopped successfully.");
        }
    }

    /**
     * Acquire any resources needed for the server.
     */
    public void open(RemoteMessageHandler messageHandler, int port) {
        if (isOpen()) {
            return;
        }

        try {
            remoteServerThread = new RemoteListenerServerThread(messageHandler, port);
        } catch (BindException e) {
            LOGGER.error("There was an error opening the configured network port {}. Please ensure there isn't another" +
                    " application already using that port.", port);
            remoteServerThread = null;
        } catch (IOException e) {
            LOGGER.error("Unknown error while opening the network port.", e);
            remoteServerThread = null;
        }
    }

    public boolean isOpen() {
        return remoteServerThread != null;
    }

    public void start() {
        if (isOpen() && isNotStartedBefore()) {
            // threads can only be started when in state NEW
            remoteServerThread.start();
        }
    }

    public boolean isNotStartedBefore() {
        // threads can only be started when in state NEW
        return (remoteServerThread == null) || (remoteServerThread.getState() == Thread.State.NEW);
    }

    public void openAndStart(RemoteMessageHandler messageHandler, int port) {
        open(messageHandler, port);
        start();
    }

    /**
     * Opens and starts the WebSocket server on the specified port.
     * The WebSocket server allows browser extensions to connect and send commands.
     */
    public void openAndStartWebSocket(RemoteMessageHandler messageHandler, int port) {
        if (isWebSocketOpen()) {
            return;
        }

        try {
            webSocketServerThread = new WebSocketListenerServerThread(messageHandler, port);
            webSocketServerThread.start();
            LOGGER.debug("WebSocket server started on port {}", port);
        } catch (BindException e) {
            LOGGER.error("There was an error opening the WebSocket port {}. Please ensure there isn't another application already using that port.", port);
            webSocketServerThread = null;
        } catch (IOException e) {
            LOGGER.error("Unknown error while opening the WebSocket port.", e);
            webSocketServerThread = null;
        }
    }

    public boolean isWebSocketOpen() {
        return webSocketServerThread != null;
    }

    /**
     * Stops and closes the WebSocket server.
     */
    public void closeAndStopWebSocket() {
        LOGGER.debug("Stopping WebSocket server");
        if (isWebSocketOpen()) {
            webSocketServerThread.interrupt();
            webSocketServerThread = null;
            LOGGER.debug("WebSocket server stopped successfully.");
        } else {
            LOGGER.debug("WebSocket server was not open, nothing to stop.");
        }
    }

    @Override
    public void close() {
        LOGGER.debug("Closing RemoteListenerServerManager");
        stop();
    }
}
