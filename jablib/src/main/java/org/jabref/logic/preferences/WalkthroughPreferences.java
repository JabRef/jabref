package org.jabref.logic.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Preferences related to the application walkthrough functionality.
 */
public class WalkthroughPreferences {
    private final BooleanProperty mainFileDirectoryCompleted;

    public WalkthroughPreferences(boolean completed) {
        this.mainFileDirectoryCompleted = new SimpleBooleanProperty(completed);
    }

    public BooleanProperty mainFileDirectoryCompletedProperty() {
        return mainFileDirectoryCompleted;
    }

    public boolean getMainFileDirectoryCompleted() {
        return mainFileDirectoryCompleted.get();
    }

    public void setMainFileDirectoryCompleted(boolean mainFileDirectoryCompleted) {
        this.mainFileDirectoryCompleted.set(mainFileDirectoryCompleted);
    }
}
