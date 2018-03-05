package org.jabref.gui.maintable;

import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

public class MainTablePreferences {
    private final boolean showGrid;
    private final ColumnPreferences columnPreferences;
    private final boolean resizeColumnsToFit;

    public MainTablePreferences(boolean showGrid, ColumnPreferences columnPreferences, boolean resizeColumnsToFit) {
        this.showGrid = showGrid;
        this.columnPreferences = columnPreferences;
        this.resizeColumnsToFit = resizeColumnsToFit;
    }

    public static MainTablePreferences from(JabRefPreferences preferences) {
        return new MainTablePreferences(
                Globals.prefs.getBoolean(JabRefPreferences.TABLE_SHOW_GRID),
                ColumnPreferences.from(preferences),
                Globals.prefs.getBoolean(JabRefPreferences.AUTO_RESIZE_MODE));
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
