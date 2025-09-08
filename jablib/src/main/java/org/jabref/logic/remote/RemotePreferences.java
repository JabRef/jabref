package org.jabref.logic.remote;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Place for handling the preferences for the remote communication
 */
public class RemotePreferences {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemotePreferences.class);

    private final IntegerProperty port;
    private final BooleanProperty useRemoteServer;

    private final IntegerProperty httpPort;
    private final BooleanProperty enableHttpServer;

    private final BooleanProperty enableLanguageServer;
    private final IntegerProperty languageServerPort;

    public RemotePreferences(int port, boolean useRemoteServer, int httpPort, boolean enableHttpServer, boolean enableLanguageServer, int languageServerPort) {
        this.port = new SimpleIntegerProperty(port);
        this.useRemoteServer = new SimpleBooleanProperty(useRemoteServer);
        this.httpPort = new SimpleIntegerProperty(httpPort);
        this.enableHttpServer = new SimpleBooleanProperty(enableHttpServer);
        this.enableLanguageServer = new SimpleBooleanProperty(enableLanguageServer);
        this.languageServerPort = new SimpleIntegerProperty(languageServerPort);
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

    public int getHttpPort() {
        return httpPort.getValue();
    }

    public IntegerProperty httpPortProperty() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort.setValue(httpPort);
    }

    public boolean isDifferentHttpPort(int otherHttpPort) {
        return getHttpPort() != otherHttpPort;
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

    public int getLanguageServerPort() {
        return languageServerPort.getValue();
    }

    public IntegerProperty languageServerPortProperty() {
        return languageServerPort;
    }

    public void setLanguageServerPort(int languageServerPort) {
        this.languageServerPort.setValue(languageServerPort);
    }

    public boolean isDifferentLanguageServerPort(int otherLanguageServerPort) {
        return getLanguageServerPort() != otherLanguageServerPort;
    }

    public boolean enableLanguageServer() {
        return enableLanguageServer.getValue();
    }

    public BooleanProperty enableLanguageServerProperty() {
        return enableLanguageServer;
    }

    public void setEnableLanguageServer(boolean enableLanguageServer) {
        this.enableLanguageServer.setValue(enableLanguageServer);
    }

    /// Gets the IP address where both the remote server and the http server are listening.
    public static InetAddress getIpAddress() throws UnknownHostException {
        return InetAddress.getByName("localhost");
    }

    public @NonNull URI getHttpServerUri() {
        try {
            return new URI("http", null, RemotePreferences.getIpAddress().getHostAddress(), getHttpPort(), null, null, null);
        } catch (UnknownHostException | URISyntaxException e) {
            LOGGER.error("Could not create HTTP server URI. Falling back to default.", e);
            try {
                return new URI("http://localhost:23119");
            } catch (URISyntaxException ex) {
                LOGGER.error("Should never happen, raw string is already valid uri");
                throw new RuntimeException(ex);
            }
        }
    }
}
