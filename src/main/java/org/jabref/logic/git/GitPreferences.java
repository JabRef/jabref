package org.jabref.logic.git;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public class GitPreferences {
    private static String passwordEncryptionKey = null;
    private final BooleanProperty gitEnabled;
    private final SimpleStringProperty username;
    private final SimpleStringProperty password;
    private final BooleanProperty passwordEncrypted;
    private final SimpleStringProperty sshDirPath;
    private final BooleanProperty hostKeyCheckDisabled;
    private final BooleanProperty pushFrequencyEnabled;

    public GitPreferences(boolean gitEnabled,
                          String username,
                          String password,
                          boolean passwordEncrypted,
                          String sshDirPath,
                          boolean hostKeyCheckDisabled,
                          boolean pushFrequencyEnabled) {
        this.gitEnabled = new SimpleBooleanProperty(gitEnabled);
        this.pushFrequencyEnabled = new SimpleBooleanProperty(pushFrequencyEnabled);
        this.username = new SimpleStringProperty(username != null ? username : ""); // Default to empty if null
        this.sshDirPath = new SimpleStringProperty(sshDirPath);
        this.password = new SimpleStringProperty(password);
        this.hostKeyCheckDisabled = new SimpleBooleanProperty(hostKeyCheckDisabled);
        this.passwordEncrypted = new SimpleBooleanProperty(passwordEncrypted);
    }

    public void setSshDirPath(String sshDirPath) {
        this.sshDirPath.set(sshDirPath);
    }

    public SimpleStringProperty getSshPathProperty() {
        return this.sshDirPath;
    }

    public String getSshDirPath() {
        return sshDirPath.get();
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
        return gitEnabled.getValue();
    }

    public BooleanProperty getGitEnabledProperty() {
        return gitEnabled;
    }

    public void setGitEnabled(boolean gitEnabled) {
        this.gitEnabled.set(gitEnabled);
    }

    public boolean isHostKeyCheckDisabled() {
        return hostKeyCheckDisabled.getValue();
    }

    public void setHostKeyCheckDisabled(boolean hostKeyCheckDisabled) {
        this.hostKeyCheckDisabled.set((hostKeyCheckDisabled));
    }

    public BooleanProperty getHostKeyCheckDisabledProperty() {
        return hostKeyCheckDisabled;
    }

    public boolean isPushFrequencyEnabled() {
        return pushFrequencyEnabled.getValue();
    }

    public BooleanProperty getPushFrequencyEnabledProperty() {
        return pushFrequencyEnabled;
    }

    public void setPushFrequencyEnabled(boolean isPushFrequencySet) {
        this.pushFrequencyEnabled.set(isPushFrequencySet);
    }

    public BooleanProperty getPasswordEncryptedProperty() {
        return passwordEncrypted;
    }

    public boolean isPasswordEncrypted() {
        return passwordEncrypted.getValue();
    }

    public void setPasswordEncrypted(boolean isPasswordEncrypted) {
        this.passwordEncrypted.set(isPasswordEncrypted);
    }

    public Optional<String> getPasswordEncryptionKey() {
        return Optional.ofNullable(passwordEncryptionKey);
    }

    public void setPasswordEncryptionKey(String key) {
        passwordEncryptionKey = key;
    }
}
