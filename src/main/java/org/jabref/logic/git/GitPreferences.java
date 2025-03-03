package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitPreferences.class);

    private final BooleanProperty autoPushEnabled;

    public GitPreferences(boolean autoPushEnabled) {
        this.autoPushEnabled = new SimpleBooleanProperty(autoPushEnabled);
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
}
