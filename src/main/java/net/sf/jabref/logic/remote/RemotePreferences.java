package net.sf.jabref.logic.remote;

import net.sf.jabref.preferences.JabRefPreferences;

/**
 * Place for handling the preferences for the remote communication
 */
public class RemotePreferences {

    private final JabRefPreferences preferences;


    public RemotePreferences(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public int getPort() {
        return preferences.getInt(JabRefPreferences.REMOTE_SERVER_PORT);
    }

    public void setPort(int port) {
        preferences.putInt(JabRefPreferences.REMOTE_SERVER_PORT, port);
    }

    public boolean useRemoteServer() {
        return preferences.getBoolean(JabRefPreferences.USE_REMOTE_SERVER);
    }

    public void setUseRemoteServer(boolean useRemoteServer) {
        preferences.putBoolean(JabRefPreferences.USE_REMOTE_SERVER, useRemoteServer);
    }

    public boolean isDifferentPort(int otherPort) {
        return getPort() != otherPort;
    }

}
