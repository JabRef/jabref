package org.jabref.logic.remote.server;

import java.io.IOException;

/**
 * Thread wrapper for WebSocketListenerServer to allow clean lifecycle management.
 */
public class WebSocketListenerServerThread extends Thread {

    private final WebSocketListenerServer server;

    public WebSocketListenerServerThread(RemoteMessageHandler messageHandler, int port) throws IOException {
        super("WebSocketListenerServerThread");
        this.server = new WebSocketListenerServer(messageHandler, port);
    }

    @Override
    public void run() {
        server.run();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        server.close();
    }
}
