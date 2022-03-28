package org.jabref.logic.remote.server;

import java.io.IOException;

import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thread wrapper is required to be able to interrupt the remote listener while listening on a port.
 */
public class RemoteListenerServerThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteListenerServerThread.class);

    private final RemoteListenerServer server;

    public RemoteListenerServerThread(MessageHandler messageHandler, int port, PreferencesService preferencesService) throws IOException {
        this.server = new RemoteListenerServer(messageHandler, port, preferencesService);
        this.setName("JabRef - Remote Listener Server on port " + port);
    }

    @Override
    public void interrupt() {
        super.interrupt();

        LOGGER.debug("Interrupting " + this.getName());
        this.server.closeServerSocket();
    }

    @Override
    public void run() {
        this.server.run();
    }
}
