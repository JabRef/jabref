/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.remote.server;

import net.sf.jabref.JabRefExecutorService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.BindException;

/**
 * Manages the RemoteListenerServerThread through typical life cycle methods.
 * <p/>
 * open -> start -> stop
 * openAndStart -> stop
 * <p/>
 * Observer: isOpen, isNotStartedBefore
 */
public class RemoteListenerServerLifecycle implements AutoCloseable {

    private RemoteListenerServerThread remoteListenerServerThread = null;

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
        return remoteListenerServerThread == null ? true : remoteListenerServerThread.getState() == Thread.State.NEW;
    }

    public void openAndStart(MessageHandler messageHandler, int port) {
        open(messageHandler, port);
        start();
    }

    @Override
    public void close() throws Exception {
        stop();
    }
}
