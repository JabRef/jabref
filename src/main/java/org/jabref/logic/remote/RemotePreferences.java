package org.jabref.logic.remote;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

    /**
     * Gets the IP address where the remote server is listening.
     */
    public static InetAddress getIpAddress() throws UnknownHostException {
        return InetAddress.getByName("localhost");
    }

}
