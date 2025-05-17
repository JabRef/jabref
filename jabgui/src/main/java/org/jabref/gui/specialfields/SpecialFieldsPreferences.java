package org.jabref.gui.specialfields;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class SpecialFieldsPreferences {

    public static final int COLUMN_RANKING_WIDTH = 5 * 16; // Width of Ranking Icon Column

    private final BooleanProperty specialFieldsEnabled;

    public SpecialFieldsPreferences(boolean specialFieldsEnabled) {
        this.specialFieldsEnabled = new SimpleBooleanProperty(specialFieldsEnabled);
    }

    public boolean isSpecialFieldsEnabled() {
        return specialFieldsEnabled.getValue();
    }

    public BooleanProperty specialFieldsEnabledProperty() {
        return specialFieldsEnabled;
    }

    public void setSpecialFieldsEnabled(boolean specialFieldsEnabled) {
        this.specialFieldsEnabled.set(specialFieldsEnabled);
    }
}
