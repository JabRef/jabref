package org.jabref.gui.maintable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class MainTablePreferences {
    private final ColumnPreferences columnPreferences;
    private final BooleanProperty resizeColumnsToFit = new SimpleBooleanProperty();
    private final BooleanProperty extraFileColumnsEnabled = new SimpleBooleanProperty();

    public MainTablePreferences(ColumnPreferences columnPreferences,
                                boolean resizeColumnsToFit,
                                boolean extraFileColumnsEnabled) {
        this.columnPreferences = columnPreferences;
        this.resizeColumnsToFit.set(resizeColumnsToFit);
        this.extraFileColumnsEnabled.set(extraFileColumnsEnabled);
    }

    public ColumnPreferences getColumnPreferences() {
        return columnPreferences;
    }

    public boolean getResizeColumnsToFit() {
        return resizeColumnsToFit.get();
    }

    public BooleanProperty resizeColumnsToFitProperty() {
        return resizeColumnsToFit;
    }

    public void setResizeColumnsToFit(boolean resizeColumnsToFit) {
        this.resizeColumnsToFit.set(resizeColumnsToFit);
    }

    public boolean getExtraFileColumnsEnabled() {
        return extraFileColumnsEnabled.get();
    }

    public BooleanProperty extraFileColumnsEnabledProperty() {
        return extraFileColumnsEnabled;
    }

    public void setExtraFileColumnsEnabled(boolean extraFileColumnsEnabled) {
        this.extraFileColumnsEnabled.set(extraFileColumnsEnabled);
    }
}
