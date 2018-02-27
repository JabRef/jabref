package org.jabref.gui.maintable;

public class MainTablePreferences {
    private final boolean showGrid;
    private final ColumnPreferences columnPreferences;
    private final boolean resizeColumnsToFit;

    public MainTablePreferences(boolean showGrid, ColumnPreferences columnPreferences, boolean resizeColumnsToFit) {
        this.showGrid = showGrid;
        this.columnPreferences = columnPreferences;
        this.resizeColumnsToFit = resizeColumnsToFit;
    }

    public ColumnPreferences getColumnPreferences() {
        return columnPreferences;
    }

    public boolean resizeColumnsToFit() {

        return resizeColumnsToFit;
    }

    public boolean showGrid() {
        return showGrid;
    }
}
