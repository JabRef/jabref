package org.jabref.logic.remote.server;

import java.io.IOException;
import java.net.BindException;

import org.jabref.gui.JabRefExecutorService;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the RemoteListenerServerThread through typical life cycle methods.
 * <p/>
 * open -> start -> stop
 * openAndStart -> stop
 * <p/>
 * Observer: isOpen, isNotStartedBefore
 */
public class RemoteListenerServerLifecycle implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteListenerServerLifecycle.class);

    private RemoteListenerServerThread remoteListenerServerThread;

    public void stop() {
        if (isOpen()) {
            remoteListenerServerThread.interrupt();
            remoteListenerServerThread = null;
            JabRefExecutorService.INSTANCE.stopRemoteThread();
        }
    }

    /**
     * Acquire any resources needed for the server.
     */
    public void open(MessageHandler messageHandler, int port, PreferencesService preferencesService) {
        if (isOpen()) {
            return;
        }

        RemoteListenerServerThread result;
        try {
            result = new RemoteListenerServerThread(messageHandler, port, preferencesService);
        } catch (BindException e) {
            LOGGER.warn("Port is blocked", e);
            result = null;
        } catch (IOException e) {
            result = null;
        }
        remoteListenerServerThread = result;
    }

    public boolean isOpen() {
        return remoteListenerServerThread != null;
    }

    public void start() {
        if (isOpen() && isNotStartedBefore()) {
            // threads can only be started when in state NEW
            JabRefExecutorService.INSTANCE.manageRemoteThread(remoteListenerServerThread);
        }
    }

    public boolean isNotStartedBefore() {
        // threads can only be started when in state NEW
        return (remoteListenerServerThread == null) || (remoteListenerServerThread.getState() == Thread.State.NEW);
    }

    public void openAndStart(MessageHandler messageHandler, int port, PreferencesService preferencesService) {
        open(messageHandler, port, preferencesService);
        start();
    }

    @Override
    public void close() {
        stop();
    }
}
