package org.jabref.logic.tele.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thread wrapper is required to be able to interrupt the tele server, e.g. when JabRef is closing down the server should shutdown as well.
 */
public class TeleServerThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeleServerThread.class);

    private final TeleServer server;

    public TeleServerThread(TeleMessageHandler messageHandler, int port) throws IOException {
        this.server = new TeleServer(messageHandler, port);
        this.setName("JabRef - Tele Listener Server on port " + port);
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
