package org.jabref.logic.tele.server;

import java.io.IOException;
import java.net.BindException;

import org.jabref.gui.JabRefExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the TeleServerThread through typical life cycle methods.
 * <p/>
 * open -> start -> stop
 * openAndStart -> stop
 * <p/>
 * Observer: isOpen, isNotStartedBefore
 */
public class TeleServerManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeleServerManager.class);

    private TeleServerThread teleServerThread;

    public void stop() {
        if (isOpen()) {
            teleServerThread.interrupt();
            teleServerThread = null;
            JabRefExecutorService.INSTANCE.stopTeleThread();
        }
    }

    /**
     * Acquire any resources needed for the server.
     */
    public void open(TeleMessageHandler messageHandler, int port) {
        if (isOpen()) {
            return;
        }

        try {
            teleServerThread = new TeleServerThread(messageHandler, port);
        } catch (BindException e) {
            LOGGER.error("There was an error opening the configured network port {}. Please ensure there isn't another" +
                    " application already using that port.", port);
            teleServerThread = null;
        } catch (IOException e) {
            LOGGER.error("Unknown error while opening the network port.", e);
            teleServerThread = null;
        }
    }

    public boolean isOpen() {
        return teleServerThread != null;
    }

    public void start() {
        if (isOpen() && isNotStartedBefore()) {
            // threads can only be started when in state NEW
            JabRefExecutorService.INSTANCE.startTeleThread(teleServerThread);
        }
    }

    public boolean isNotStartedBefore() {
        // threads can only be started when in state NEW
        return (teleServerThread == null) || (teleServerThread.getState() == Thread.State.NEW);
    }

    public void openAndStart(TeleMessageHandler messageHandler, int port) {
        open(messageHandler, port);
        start();
    }

    @Override
    public void close() {
        stop();
    }
}
