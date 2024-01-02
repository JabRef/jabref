package org.jabref.logic.net;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProxyPreferences {

    private final BooleanProperty useProxy;
    private final StringProperty hostname;
    private final StringProperty port;
    private final BooleanProperty useAuthentication;
    private final StringProperty username;
    private final StringProperty password;
    private final BooleanProperty persistPassword;

    public ProxyPreferences(Boolean useProxy,
                            String hostname,
                            String port,
                            Boolean useAuthentication,
                            String username,
                            String password,
                            boolean persistPassword) {
        this.useProxy = new SimpleBooleanProperty(useProxy);
        this.hostname = new SimpleStringProperty(hostname);
        this.port = new SimpleStringProperty(port);
        this.useAuthentication = new SimpleBooleanProperty(useAuthentication);
        this.username = new SimpleStringProperty(username);
        this.password = new SimpleStringProperty(password);
        this.persistPassword = new SimpleBooleanProperty(persistPassword);
    }

    public final boolean shouldUseProxy() {
        return useProxy.getValue();
    }

    public BooleanProperty useProxyProperty() {
        return useProxy;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy.set(useProxy);
    }

    public final String getHostname() {
        return hostname.getValue();
    }

    public StringProperty hostnameProperty() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname.set(hostname);
    }

    public final String getPort() {
        return port.getValue();
    }

    public StringProperty portProperty() {
        return port;
    }

    public void setPort(String port) {
        this.port.set(port);
    }

    public final boolean shouldUseAuthentication() {
        return useAuthentication.get();
    }

    public BooleanProperty useAuthenticationProperty() {
        return useAuthentication;
    }

    public void setUseAuthentication(boolean useAuthentication) {
        this.useAuthentication.set(useAuthentication);
    }

    public final String getUsername() {
        return username.getValue();
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public final String getPassword() {
        return password.getValue();
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public boolean shouldPersistPassword() {
        return persistPassword.get();
    }

    public BooleanProperty persistPasswordProperty() {
        return persistPassword;
    }

    public void setPersistPassword(boolean persistPassword) {
        this.persistPassword.set(persistPassword);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProxyPreferences other = (ProxyPreferences) o;
        return Objects.equals(useProxy.getValue(), other.useProxy.getValue())
                && Objects.equals(hostname.getValue(), other.hostname.getValue())
                && Objects.equals(port.getValue(), other.port.getValue())
                && Objects.equals(useAuthentication.getValue(), other.useAuthentication.getValue())
                && Objects.equals(username.getValue(), other.username.getValue())
                && Objects.equals(password.getValue(), other.password.getValue())
                && Objects.equals(persistPassword.getValue(), other.persistPassword.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                useProxy.getValue(),
                hostname.getValue(),
                port.getValue(),
                useAuthentication.getValue(),
                username.getValue(),
                password.getValue(),
                persistPassword.getValue());
    }
}
