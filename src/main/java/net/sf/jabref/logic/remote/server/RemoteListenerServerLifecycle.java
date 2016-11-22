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

    private RemoteListenerServerRunnable remoteListenerServerStop;

    private static final Log LOGGER = LogFactory.getLog(RemoteListenerServerLifecycle.class);

    public void stop() {
        if (isOpen()) {
            remoteListenerServerStop.stopServer();
            remoteListenerServerStop = null;
        }
    }

    /**
     * Acquire any resources needed for the server.
     */
    public void open(MessageHandler messageHandler, int port) {
        if (isOpen()) {
            return;
        }

        RemoteListenerServerRunnable result;
        try {
            result = new RemoteListenerServerRunnable(messageHandler, port);
        } catch (BindException e) {
            LOGGER.warn("Port is blocked", e);
            result = null;
        } catch (IOException e) {
            result = null;
        }
        remoteListenerServerStop = result;
    }

    public boolean isOpen() {
        return remoteListenerServerStop != null;
    }

    public void start() {
        if (isOpen()) {
            // threads can only be started when in state NEW
            JabRefExecutorService.INSTANCE.executeInterruptableTask(remoteListenerServerStop);
        }
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
