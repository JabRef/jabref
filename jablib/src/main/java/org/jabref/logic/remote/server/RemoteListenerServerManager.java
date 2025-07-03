package org.jabref.logic.remote.server;

import java.io.IOException;
import java.net.BindException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the RemoteServerThread through typical life cycle methods.
///
/// open -> start -> stop
/// openAndStart -> stop
///
/// Observer: isOpen, isNotStartedBefore
public class RemoteListenerServerManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteListenerServerManager.class);

    private RemoteListenerServerThread remoteServerThread;

    public void stop() {
        LOGGER.debug("Stopping RemoteListenerServerManager");
        if (isOpen()) {
            remoteServerThread.interrupt();
            remoteServerThread = null;
            LOGGER.debug("RemoteListenerServerManager stopped successfully.");
        } else {
            LOGGER.debug("RemoteListenerServerManager was not open, nothing to stop.");
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

    @Override
    public void close() {
        LOGGER.debug("Closing RemoteListenerServerManager");
        stop();
    }
}
