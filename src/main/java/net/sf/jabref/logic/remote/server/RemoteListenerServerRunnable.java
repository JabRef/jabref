package net.sf.jabref.logic.remote.server;

import java.io.IOException;

/**
 * This thread wrapper is required to be able to interrupt the remote listener while listening on a port.
 */
public class RemoteListenerServerRunnable implements Runnable {

    private final RemoteListenerServer server;

    private final String name;

    public RemoteListenerServerRunnable(MessageHandler messageHandler, int port) throws IOException {
        this.server = new RemoteListenerServer(messageHandler, port);
        this.name = "JabRef - Remote Listener Server on port " + port;
    }

    @Override
    public void run() {
        this.server.run();
    }

    public String getName() {
        return this.name;
    }

    public void stopServer(){
        server.stopServer();
        server.closeServerSocket();
    }

}
