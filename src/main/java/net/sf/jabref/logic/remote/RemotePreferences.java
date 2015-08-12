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
package net.sf.jabref.logic.remote;

import net.sf.jabref.JabRefPreferences;

/**
 * Place for handling the preferences for the remote communication
 */
public class RemotePreferences {

    public static final String REMOTE_SERVER_PORT = "remoteServerPort";
    public static final String USE_REMOTE_SERVER = "useRemoteServer";

    private final JabRefPreferences preferences;

    public RemotePreferences(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public int getPort() {
        return preferences.getInt(REMOTE_SERVER_PORT);
    }

    public void setPort(int port) {
        preferences.putInt(REMOTE_SERVER_PORT, port);
    }

    public boolean useRemoteServer() {
        return preferences.getBoolean(USE_REMOTE_SERVER);
    }

    public void setUseRemoteServer(boolean useRemoteServer) {
        preferences.putBoolean(USE_REMOTE_SERVER, useRemoteServer);
    }

    public boolean isDifferentPort(int otherPort) {
        return getPort() != otherPort;
    }

}
