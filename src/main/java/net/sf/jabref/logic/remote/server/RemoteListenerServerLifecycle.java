package net.sf.jabref.logic.remote.server;

import java.io.IOException;
import java.net.BindException;

import net.sf.jabref.JabRefExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages the RemoteListenerServerThread through typical life cycle methods.
 * <p/>
 * open -> start -> stop
 * openAndStart -> stop
 * <p/>
 * Observer: isOpen, isNotStartedBefore
 */
public class RemoteListenerServerLifecycle implements AutoCloseable {

    private RemoteListenerServerThread remoteListenerServerThread;

    private static final Log LOGGER = LogFactory.getLog(RemoteListenerServerLifecycle.class);

    public void stop() {
        if (isOpen()) {
            remoteListenerServerThread.interrupt();
            remoteListenerServerThread = null;
        }
    }

    /**
     * Acquire any resources needed for the server.
     */
    public void open(MessageHandler messageHandler, int port) {
        if (isOpen()) {
            return;
        }

        RemoteListenerServerThread result;
        try {
            result = new RemoteListenerServerThread(messageHandler, port);
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
            JabRefExecutorService.INSTANCE.executeInOwnThread(remoteListenerServerThread);
        }
    }

    public boolean isNotStartedBefore() {
        // threads can only be started when in state NEW
        return (remoteListenerServerThread == null) || (remoteListenerServerThread.getState() == Thread.State.NEW);
    }

    public void openAndStart(MessageHandler messageHandler, int port) {
        open(messageHandler, port);
        start();
    }

    @Override
    public void close() {
        stop();
    }
}
