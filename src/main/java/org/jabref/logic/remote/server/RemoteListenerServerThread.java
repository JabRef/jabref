package org.jabref.logic.remote.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thread wrapper is required to be able to interrupt the remote listener server, e.g. when JabRef is closing down the server should shutdown as well.
 */
public class RemoteListenerServerThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteListenerServerThread.class);

    private final RemoteListenerServer server;

    public RemoteListenerServerThread(RemoteMessageHandler messageHandler, int port) throws IOException {
        this.server = new RemoteListenerServer(messageHandler, port);
        this.setName("JabRef - Remote Listener Server on port " + port);
    }

    @Override
    public void interrupt() {
        LOGGER.debug("Interrupting " + this.getName());
        this.server.closeServerSocket();
        super.interrupt();
    }

    @Override
    public void run() {
        this.server.run();
    }
}
