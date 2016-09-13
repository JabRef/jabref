package net.sf.jabref.logic.remote;

/**
 * Place for handling the preferences for the remote communication
 */
public class RemotePreferences {

    private int port;
    private boolean useRemoteServer;


    public RemotePreferences(int port, boolean useRemoteServer) {
        this.port = port;
        this.useRemoteServer = useRemoteServer;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean useRemoteServer() {
        return useRemoteServer;
    }

    public void setUseRemoteServer(boolean useRemoteServer) {
        this.useRemoteServer = useRemoteServer;
    }

    public boolean isDifferentPort(int otherPort) {
        return getPort() != otherPort;
    }

}
