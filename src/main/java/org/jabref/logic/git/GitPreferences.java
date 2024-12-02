package org.jabref.logic.git;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public class GitPreferences {
    private static String passwordEncryptionKey = null;
    private static String sshPassphrase = null;
    private final BooleanProperty gitEnabled;
    private final SimpleStringProperty username;
    private final SimpleStringProperty password;
    private final BooleanProperty passwordEncrypted;
    private final SimpleStringProperty sshDirPath;
    private final SimpleStringProperty pushFrequency;
    private final BooleanProperty sshKeyEncrypted;
    private final BooleanProperty hostKeyCheckDisabled;
    private final BooleanProperty pushFrequencyEnabled;

    public GitPreferences(boolean gitEnabled,
                          String username,
                          String password,
                          boolean passwordEncrypted,
                          String sshDirPath,
                          boolean sshKeyEncrypted,
                          boolean hostKeyCheckDisabled,
                          boolean pushFrequencyEnabled,
                          String pushFrequency) {
        this.gitEnabled = new SimpleBooleanProperty(gitEnabled);
        this.pushFrequencyEnabled = new SimpleBooleanProperty(pushFrequencyEnabled);
        this.username = new SimpleStringProperty(username != null ? username : ""); // Default to empty if null
        this.sshDirPath = new SimpleStringProperty(sshDirPath);
        this.password = new SimpleStringProperty(password);
        this.hostKeyCheckDisabled = new SimpleBooleanProperty(hostKeyCheckDisabled);
        this.passwordEncrypted = new SimpleBooleanProperty(passwordEncrypted);
        this.sshKeyEncrypted = new SimpleBooleanProperty(sshKeyEncrypted);
        this.pushFrequency = new SimpleStringProperty(pushFrequency);
    }

    public void setSshDirPath(String sshDirPath) {
        this.sshDirPath.set(sshDirPath);
    }

    public SimpleStringProperty getSshPathProperty() {
        return this.sshDirPath;
    }

    public Optional<String> getSshDirPath() {
        return Optional.ofNullable(sshDirPath.get()).filter(s -> !s.isBlank());
    }

    public void setPushFrequency(String pushFrequency) {
        this.pushFrequency.set(pushFrequency);
    }

    public SimpleStringProperty getPushFrequencyProperty() {
        return this.pushFrequency;
    }

    public Optional<String> getPushFrequency() {
        return Optional.ofNullable(pushFrequency.get()).filter(s -> !s.isBlank());
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public SimpleStringProperty getPasswordProperty() {
        return this.password;
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password.get()).filter(s -> !s.isBlank());
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public SimpleStringProperty getUsernameProperty() {
        return username;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username.get()).filter(s -> !s.isBlank());
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
        this.hostKeyCheckDisabled.set(hostKeyCheckDisabled);
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

    public BooleanProperty getSshKeyEncryptedProperty() {
        return sshKeyEncrypted;
    }

    public boolean isSshKeyEncrypted() {
        return sshKeyEncrypted.getValue();
    }

    public void setSshKeyEncrypted(boolean isSshKeyEncrypted) {
        this.sshKeyEncrypted.set(isSshKeyEncrypted);
    }

//  -------------------

    public static Optional<String> getPasswordEncryptionKey() {
        return Optional.ofNullable(passwordEncryptionKey).filter(s -> !s.isBlank());
    }

    public static void setPasswordEncryptionKey(String key) {
        passwordEncryptionKey = key;
    }

    public static Optional<String> getSshPassphrase() {
        return Optional.ofNullable(sshPassphrase).filter(s -> !s.isBlank());
    }

    public static void setSshPassphrase(String sshPassphrase) {
        GitPreferences.sshPassphrase = sshPassphrase;
    }
}
