package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class GitPreferences {
    private final BooleanProperty gitSupportEnabledProperty;
    private final BooleanProperty frequencyLabelEnabledProperty;

    private final BooleanProperty hostKeyCheckProperty;
    private final ObjectProperty<AuthenticationViewMode> authenticationMethod;
    private SimpleStringProperty sshPath;

    // TODO: encrypt
    private final SimpleStringProperty password;

    private final SimpleStringProperty username;

    public GitPreferences(boolean gitSupportEnabledProperty, AuthenticationViewMode authenticationMethod, boolean frequencyLabelEnabledProperty, String username, String sshPath, String password, boolean hostKeyCheckProperty) {
        this.gitSupportEnabledProperty = new SimpleBooleanProperty(gitSupportEnabledProperty);
        this.authenticationMethod = new SimpleObjectProperty<>(authenticationMethod);
        this.frequencyLabelEnabledProperty = new SimpleBooleanProperty(frequencyLabelEnabledProperty);
        this.username = new SimpleStringProperty(username != null ? username : ""); // Default to empty if null
        this.sshPath = new SimpleStringProperty(sshPath);
        this.password = new SimpleStringProperty(password);
        this.hostKeyCheckProperty = new SimpleBooleanProperty(hostKeyCheckProperty);
    }

    public AuthenticationViewMode getAuthenticationMethod() {
        return authenticationMethod.getValue();
    }

    public ObjectProperty<AuthenticationViewMode> getAuthenticationProperty() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(AuthenticationViewMode authenticationMethod) {
        this.authenticationMethod.set(authenticationMethod);
    }

    public void setSshPath(String sshPath) {
        this.sshPath.set(sshPath);
    }

    public SimpleStringProperty getSshPathProperty() {
        return this.sshPath;
    }

    public String getSshPath() {
        System.out.println("Get Method SSH PATH is " + sshPath.get());
        return sshPath.get();
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public SimpleStringProperty getPasswordProperty() {
        return this.password;
    }

    public String getPassword() {
        return this.password.get();
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public SimpleStringProperty getUsernameProperty() {
        System.out.println("Get Method Username is " + username);
        return username;
    }

    public String getUsername() {
        return username.get();
    }

    public boolean isGitEnabled() {
        return gitSupportEnabledProperty.getValue();
    }

    public boolean isFrequencyLabelEnabled() {
        return frequencyLabelEnabledProperty.getValue();
    }

    public boolean isHostKeyCheckEnabled() {
        return hostKeyCheckProperty.getValue();
    }

    public void setHostKeyCheckProperty(boolean hostKeyCheckProperty) {
        this.hostKeyCheckProperty.set((hostKeyCheckProperty));
    }

    public BooleanProperty getHostKeyCheckProperty() {
        return hostKeyCheckProperty;
    }

    public BooleanProperty getGitSupportEnabledProperty() {
        return gitSupportEnabledProperty;
    }

    public BooleanProperty getFrequencyLabelEnabledProperty() {
        return frequencyLabelEnabledProperty;
    }

    public void setGitSupportEnabledProperty(boolean gitSupportEnabledProperty) {
        this.gitSupportEnabledProperty.set(gitSupportEnabledProperty);
    }

    public void setFrequencyLabelEnabledProperty(boolean frequencyLabelEnabledProperty) {
        this.frequencyLabelEnabledProperty.set(frequencyLabelEnabledProperty);
    }
}
