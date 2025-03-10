package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.preferences.AutoPushMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitPreferences.class);

    private final BooleanProperty autoPushEnabled;
    private final ObjectProperty<AutoPushMode> autoPushMode;

    public GitPreferences(boolean autoPushEnabled, AutoPushMode autoPushMode) {
        this.autoPushEnabled = new SimpleBooleanProperty(autoPushEnabled);
        this.autoPushMode = new SimpleObjectProperty<>(autoPushMode);
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

    public GitPreferences withAutoPushEnabled(boolean enabled) {
        return new GitPreferences(enabled);
    }

    public AutoPushMode getAutoPushMode() {
        return autoPushMode.get();
    }

    public ObjectProperty<AutoPushMode> getAutoPushModeProperty() {
        return autoPushMode;
    }
}
