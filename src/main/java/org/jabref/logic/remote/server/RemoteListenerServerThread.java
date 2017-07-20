package org.jabref.logic.remote.server;

import java.io.IOException;

import org.jabref.Logger;

/**
 * This thread wrapper is required to be able to interrupt the remote listener while listening on a port.
 */
public class RemoteListenerServerThread extends Thread {


    private final RemoteListenerServer server;

    public RemoteListenerServerThread(MessageHandler messageHandler, int port) throws IOException {
        this.server = new RemoteListenerServer(messageHandler, port);
        this.setName("JabRef - Remote Listener Server on port " + port);
    }

    @Override
    public void interrupt() {
        super.interrupt();

        Logger.debug(this, "Interrupting " + this.getName());
        this.server.closeServerSocket();
    }

    @Override
    public void run() {
        this.server.run();
    }

}
