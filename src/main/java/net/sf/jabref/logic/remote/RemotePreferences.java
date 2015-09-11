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
