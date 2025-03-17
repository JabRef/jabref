package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.preferences.AutoPushMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitPreferences.class);

    private StringProperty username;
    private StringProperty password;
    private final BooleanProperty autoPushEnabled;
    private final ObjectProperty<AutoPushMode> autoPushMode;

    public GitPreferences(String username,
                          String password,
                          boolean autoPushEnabled,
                          AutoPushMode autoPushMode) {
        this.username = new SimpleStringProperty(username);
        this.password = new SimpleStringProperty(password);
        this.autoPushEnabled = new SimpleBooleanProperty(autoPushEnabled);
        this.autoPushMode = new SimpleObjectProperty<>(autoPushMode);
    }

    public StringProperty getUsernameProperty() {
        return this.username;
    }

    public String getUsername() {
        return this.username.get();
    }

    public StringProperty getPasswordProperty() {
        return this.password;
    }

    public String getPassword() {
        return this.password.get();
    }

    public void setPassword(StringProperty password) {
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = new SimpleStringProperty(password);
    }

    public void setUsername(StringProperty username) {
        this.username = username;
    }

    public void setUsername(String username) {
        this.username = new SimpleStringProperty(username);
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
}
