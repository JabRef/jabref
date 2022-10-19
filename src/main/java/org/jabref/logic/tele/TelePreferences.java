package org.jabref.logic.tele;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Place for handling the preferences for the tele communication
 */
public class TelePreferences {

    private IntegerProperty port;
    private BooleanProperty shouldUseTeleServer;

    public TelePreferences(int port, boolean shouldUseTeleServer) {
        this.port = new SimpleIntegerProperty(port);
        this.shouldUseTeleServer = new SimpleBooleanProperty(shouldUseTeleServer);
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

    public boolean shouldUseTeleServer() {
        return shouldUseTeleServer.getValue();
    }

    public BooleanProperty shouldUseTeleServerProperty() {
        return shouldUseTeleServer;
    }

    public void setShouldUseTeleServer(boolean shouldUseTeleServer) {
        this.shouldUseTeleServer.setValue(shouldUseTeleServer);
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
