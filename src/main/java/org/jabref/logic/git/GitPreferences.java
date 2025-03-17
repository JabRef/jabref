package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

import org.jabref.logic.preferences.AutoPushMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitPreferences.class);

    private final BooleanProperty autoPushEnabled;
    private final ObjectProperty<AutoPushMode> autoPushMode;
    private final StringProperty gitHubUsername;
    private final StringProperty gitHubPasskey;

    public GitPreferences(boolean autoPushEnabled, AutoPushMode autoPushMode, String gitHubUsername, String gitHubPasskey) {
        this.autoPushEnabled = new SimpleBooleanProperty(autoPushEnabled);
        this.autoPushMode = new SimpleObjectProperty<>(autoPushMode);
        this.gitHubUsername = new SimpleStringProperty(gitHubUsername);
        this.gitHubPasskey = new SimpleStringProperty(gitHubPasskey);
    }

    public boolean getAutoPushEnabled() {
        return autoPushEnabled.get();
    }

    public BooleanProperty getAutoPushEnabledProperty() {
        return autoPushEnabled;
    }

    public void setAutoPushEnabled(boolean enabled) {
        autoPushEnabled.set(enabled);
    }

    public AutoPushMode getAutoPushMode() {
        return autoPushMode.get();
    }

    public ObjectProperty<AutoPushMode> getAutoPushModeProperty() {
        return autoPushMode;
    }

    public void setAutoPushMode(AutoPushMode mode) {
        autoPushMode.set(mode);
    }

    public String getGitHubUsername() {
        return gitHubUsername.get();
    }

    public void setGitHubUsername(String username) {
        gitHubUsername.set(username);
    }

    public StringProperty gitHubUsernameProperty() {
        return gitHubUsername;
    }

    public String getGitHubPasskey() {
        return gitHubPasskey.get();
    }

    public void setGitHubPasskey(String passkey) {
        gitHubPasskey.set(passkey);
    }

    public StringProperty gitHubPasskeyProperty() {
        return gitHubPasskey;
    }
}
