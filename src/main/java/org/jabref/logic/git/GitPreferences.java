package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GitPreferences {
    private final BooleanProperty autoPushEnabled;
    private final StringProperty gitHubUsername;
    private final StringProperty gitHubPasskey;

    public GitPreferences(boolean autoPushEnabled, String gitHubUsername, String gitHubPasskey) {
        this.autoPushEnabled = new SimpleBooleanProperty(autoPushEnabled);
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
