/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.remote.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * This thread wrapper is required to be able to interrupt the remote listener while listening on a port.
 */
public class RemoteListenerServerThread extends Thread {

    private static final Log LOGGER = LogFactory.getLog(RemoteListenerServerThread.class);

    private final RemoteListenerServer server;

    public RemoteListenerServerThread(MessageHandler messageHandler, int port) throws IOException {
        this.server = new RemoteListenerServer(messageHandler, port);
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
