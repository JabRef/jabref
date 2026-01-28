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

    private CopyToPreferences() {
        this(
                true, // shouldAskForIncludingCrossReferences
                false // shouldIncludeCrossReferences
        );
    }

    public static CopyToPreferences getDefault() {
        return new CopyToPreferences();
    }

    public void setAll(CopyToPreferences other) {
        this.shouldIncludeCrossReferences.set(other.getShouldIncludeCrossReferences());
        this.shouldAskForIncludingCrossReferences.set(other.getShouldAskForIncludingCrossReferences());
    }

    public boolean getShouldIncludeCrossReferences() {
        return shouldIncludeCrossReferences.get();
    }

    public void setShouldIncludeCrossReferences(boolean decision) {
        this.shouldIncludeCrossReferences.set(decision);
    }

    public BooleanProperty shouldIncludeCrossReferencesProperty() {
        return shouldIncludeCrossReferences;
    }

    public boolean getShouldAskForIncludingCrossReferences() {
        return shouldAskForIncludingCrossReferences.get();
    }

    public void setShouldAskForIncludingCrossReferences(boolean decision) {
        this.shouldAskForIncludingCrossReferences.set(decision);
    }

    public BooleanProperty shouldAskForIncludingCrossReferencesProperty() {
        return shouldAskForIncludingCrossReferences;
    }
}
