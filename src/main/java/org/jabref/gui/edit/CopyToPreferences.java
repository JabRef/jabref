package org.jabref.gui.edit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class CopyToPreferences {
    private final BooleanProperty shouldIncludeCrossReferences = new SimpleBooleanProperty();
    private final BooleanProperty shouldAskForIncludingCrossReferences = new SimpleBooleanProperty();

    public CopyToPreferences(boolean shouldAskForIncludingCrossReferences, boolean shouldIncludeCrossReferences) {
        this.shouldIncludeCrossReferences.set(shouldIncludeCrossReferences);
        this.shouldAskForIncludingCrossReferences.set(shouldAskForIncludingCrossReferences);
    }

    public boolean getShouldIncludeCrossReferences() {
        return shouldIncludeCrossReferences.get();
    }

    public void setShouldIncludeCrossReferences(boolean decision) {
        this.shouldIncludeCrossReferences.set(decision);
    }

    public boolean getShouldAskForIncludingCrossReferences() {
        return shouldAskForIncludingCrossReferences.get();
    }

    public void setShouldAskForIncludingCrossReferences(boolean decision) {
        this.shouldAskForIncludingCrossReferences.set(decision);
    }
}
