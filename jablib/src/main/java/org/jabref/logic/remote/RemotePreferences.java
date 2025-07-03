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

    private final IntegerProperty port;
    private final BooleanProperty useRemoteServer;

    private final BooleanProperty enableHttpServer;

    public RemotePreferences(int port, boolean useRemoteServer, boolean enableHttpServer) {
        this.port = new SimpleIntegerProperty(port);
        this.useRemoteServer = new SimpleBooleanProperty(useRemoteServer);
        this.enableHttpServer = new SimpleBooleanProperty(enableHttpServer);
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

    public boolean isDifferentPort(int otherPort) {
        return getPort() != otherPort;
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

    public boolean enableHttpServer() {
        return enableHttpServer.getValue();
    }

    public BooleanProperty enableHttpServerProperty() {
        return enableHttpServer;
    }

    public void setEnableHttpServer(boolean enableHttpServer) {
        this.enableHttpServer.setValue(enableHttpServer);
    }

    /// Gets the IP address where both the remote server and the http server are listening.
    public static InetAddress getIpAddress() throws UnknownHostException {
        return InetAddress.getByName("localhost");
    }
}
