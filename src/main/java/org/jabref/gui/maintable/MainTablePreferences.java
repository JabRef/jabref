package org.jabref.gui.maintable;

public class MainTablePreferences {
    private final ColumnPreferences columnPreferences;
    private final boolean resizeColumnsToFit;
    private final boolean extraFileColumnsEnabled;

    public MainTablePreferences(ColumnPreferences columnPreferences, boolean resizeColumnsToFit, boolean extraFileColumnsEnabled) {
        this.columnPreferences = columnPreferences;
        this.resizeColumnsToFit = resizeColumnsToFit;
        this.extraFileColumnsEnabled = extraFileColumnsEnabled;
    }

    public ColumnPreferences getColumnPreferences() {
        return columnPreferences;
    }

    public boolean getResizeColumnsToFit() {
        return resizeColumnsToFit;
    }

    public boolean getExtraFileColumnsEnabled() {
        return extraFileColumnsEnabled;
    }
}
