package org.jabref.logic.remote;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Place for handling the preferences for the remote communication
public class RemotePreferences {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemotePreferences.class);

    private final BooleanProperty enableRemoteServer;
    private final IntegerProperty remoteServerPort;

    private final BooleanProperty enableHttpServer;
    private final IntegerProperty httpServerPort;

    private final BooleanProperty enableLanguageServer;
    private final IntegerProperty languageServerPort;

    private final ObservableList<String> allowedOrigins;
    private final StringProperty apiToken;
    private final BooleanProperty directHttpImport;
    private final BooleanProperty allowUnauthenticatedAccessWithoutOrigin;

    public RemotePreferences(boolean enableRemoteServer,
                             int remoteServerPort,
                             boolean enableHttpServer,
                             int httpServerPort,
                             boolean enableLanguageServer,
                             int languageServerPort,
                             List<String> allowedOrigins,
                             String apiToken,
                             boolean directHttpImport,
                             boolean allowUnauthenticatedAccessWithoutOrigin) {
        this.enableRemoteServer = new SimpleBooleanProperty(enableRemoteServer);
        this.remoteServerPort = new SimpleIntegerProperty(remoteServerPort);
        this.enableHttpServer = new SimpleBooleanProperty(enableHttpServer);
        this.httpServerPort = new SimpleIntegerProperty(httpServerPort);
        this.enableLanguageServer = new SimpleBooleanProperty(enableLanguageServer);
        this.languageServerPort = new SimpleIntegerProperty(languageServerPort);
        this.allowedOrigins = FXCollections.observableArrayList(allowedOrigins);
        this.apiToken = new SimpleStringProperty(apiToken);
        this.directHttpImport = new SimpleBooleanProperty(directHttpImport);
        this.allowUnauthenticatedAccessWithoutOrigin = new SimpleBooleanProperty(allowUnauthenticatedAccessWithoutOrigin);
    }

    private RemotePreferences() {
        this(
                true,  // enableRemoteServer
                6050,  // remoteServerPort
                false, // enableHttpServer
                23119, // httpServerPort
                false, // enableLanguageServer
                2087,  // languageServerPort
                List.of("chrome-extension://", "moz-extension://", "https://jabref.github.io", "https://jabref.org"), // allowed origins
                "", // api token
                false,  // directHttpImport,
                false // allowUnauthenticatedAccessWithoutOrigin

        );
    }

    public static RemotePreferences getDefault() {
        return new RemotePreferences();
    }

    public void setAll(RemotePreferences preferences) {
        enableRemoteServer.setValue(preferences.shouldEnableRemoteServer());
        remoteServerPort.setValue(preferences.getRemoteServerPort());
        enableHttpServer.setValue(preferences.shouldEnableHttpServer());
        httpServerPort.setValue(preferences.getHttpServerPort());
        enableLanguageServer.setValue(preferences.shouldEnableLanguageServer());
        languageServerPort.setValue(preferences.getLanguageServerPort());
        directHttpImport.setValue(preferences.directHttpImport());
    }

    public boolean shouldEnableRemoteServer() {
        return enableRemoteServer.getValue();
    }

    public BooleanProperty enableRemoteServerProperty() {
        return enableRemoteServer;
    }

    public void setEnableRemoteServer(boolean enableRemoteServer) {
        this.enableRemoteServer.setValue(enableRemoteServer);
    }

    public int getRemoteServerPort() {
        return remoteServerPort.getValue();
    }

    public IntegerProperty remoteServerPortProperty() {
        return remoteServerPort;
    }

    public void setRemoteServerPort(int remoteServerPort) {
        this.remoteServerPort.setValue(remoteServerPort);
    }

    public boolean isDifferentRemoteServerPort(int otherPort) {
        return getRemoteServerPort() != otherPort;
    }

    public boolean shouldEnableHttpServer() {
        return enableHttpServer.getValue();
    }

    public BooleanProperty enableHttpServerProperty() {
        return enableHttpServer;
    }

    public void setEnableHttpServer(boolean enableHttpServer) {
        this.enableHttpServer.setValue(enableHttpServer);
    }

    public int getHttpServerPort() {
        return httpServerPort.getValue();
    }

    public IntegerProperty httpServerPortProperty() {
        return httpServerPort;
    }

    public void setHttpServerPort(int httpServerPort) {
        this.httpServerPort.setValue(httpServerPort);
    }

    public boolean isDifferentHttpServerPort(int otherHttpPort) {
        return getHttpServerPort() != otherHttpPort;
    }

    public boolean shouldEnableLanguageServer() {
        return enableLanguageServer.getValue();
    }

    public BooleanProperty enableLanguageServerProperty() {
        return enableLanguageServer;
    }

    public void setEnableLanguageServer(boolean enableLanguageServer) {
        this.enableLanguageServer.setValue(enableLanguageServer);
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

    public ObservableList<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins.setAll(allowedOrigins);
    }

    public String getApiToken() {
        return apiToken.getValue();
    }

    public StringProperty apiTokenProperty() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken.setValue(apiToken);
    }

    public boolean directHttpImport() {
        return directHttpImport.getValue();
    }

    public BooleanProperty directHttpImportProperty() {
        return directHttpImport;
    }

    public void setDirectHttpImport(boolean directHttpImport) {
        this.directHttpImport.setValue(directHttpImport);
    }

    /// When true, HTTP API requests without an {@code Origin} header skip bearer validation (for local tools such as CAYW).
    /// Browser requests that include {@code Origin} are always subject to origin rules and extension authentication.
    public boolean allowUnauthenticatedAccessWithoutOrigin() {
        return allowUnauthenticatedAccessWithoutOrigin.getValue();
    }

    public BooleanProperty allowUnauthenticatedAccessWithoutOriginProperty() {
        return allowUnauthenticatedAccessWithoutOrigin;
    }

    public void setAllowUnauthenticatedAccessWithoutOrigin(boolean allow) {
        this.allowUnauthenticatedAccessWithoutOrigin.setValue(allow);
    }

    /// Gets the IP address where both the remote server and the http server are listening.
    public static InetAddress getIpAddress() throws UnknownHostException {
        return InetAddress.getByName("localhost");
    }

    public @NonNull URI getHttpServerUri() {
        try {
            return new URI("http", null, RemotePreferences.getIpAddress().getHostAddress(), getHttpServerPort(), null, null, null);
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
