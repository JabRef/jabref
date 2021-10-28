package org.jabref.logic.remote;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Place for handling the preferences for the remote communication
 */
public class RemotePreferences {

    private IntegerProperty port;
    private BooleanProperty useRemoteServer;

    public RemotePreferences(int port, boolean useRemoteServer) {
        this.port = new SimpleIntegerProperty(port);
        this.useRemoteServer = new SimpleBooleanProperty(useRemoteServer);
    }

    public int getPort() {
        return port.getValue();
    }

    public IntegerProperty portProperty() {
        return port;
    }

    public void setPort(int port) {
        this.port.setValue(port);
    }

    public boolean useRemoteServer() {
        return useRemoteServer.getValue();
    }

    public BooleanProperty useRemoteServerProperty() {
        return useRemoteServer;
    }

    public void setUseRemoteServer(boolean useRemoteServer) {
        this.useRemoteServer.setValue(useRemoteServer);
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
