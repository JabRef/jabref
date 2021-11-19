package org.jabref.gui.maintable;

public class MainTablePreferences {
    private final MainTableColumnPreferences columnPreferences;
    private final boolean resizeColumnsToFit;
    private final boolean extraFileColumnsEnabled;

    public MainTablePreferences(MainTableColumnPreferences columnPreferences, boolean resizeColumnsToFit, boolean extraFileColumnsEnabled) {
        this.columnPreferences = columnPreferences;
        this.resizeColumnsToFit = resizeColumnsToFit;
        this.extraFileColumnsEnabled = extraFileColumnsEnabled;
    }

    public MainTableColumnPreferences getColumnPreferences() {
        return columnPreferences;
    }

    public boolean getResizeColumnsToFit() {
        return resizeColumnsToFit;
    }

    public boolean getExtraFileColumnsEnabled() {
        return extraFileColumnsEnabled;
    }
}
