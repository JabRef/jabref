package org.jabref.logic.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Preferences related to the application walkthrough functionality.
 */
public class WalkthroughPreferences {
    private final BooleanProperty completed;

    public WalkthroughPreferences(boolean completed) {
        this.completed = new SimpleBooleanProperty(completed);
    }

    public BooleanProperty completedProperty() {
        return completed;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public void setCompleted(boolean completed) {
        this.completed.set(completed);
    }
}
