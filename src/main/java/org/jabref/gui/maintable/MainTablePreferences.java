package org.jabref.gui.maintable;

public class MainTablePreferences {
    private final ColumnPreferences columnPreferences;
    private final boolean resizeColumnsToFit;

    public MainTablePreferences(ColumnPreferences columnPreferences, boolean resizeColumnsToFit) {
        this.columnPreferences = columnPreferences;
        this.resizeColumnsToFit = resizeColumnsToFit;
    }

    public ColumnPreferences getColumnPreferences() {
        return columnPreferences;
    }

    public boolean resizeColumnsToFit() {

        return resizeColumnsToFit;
    }
}
