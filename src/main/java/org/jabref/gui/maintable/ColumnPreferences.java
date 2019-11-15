package org.jabref.gui.maintable;

import java.util.List;
import java.util.Map;

import javafx.scene.control.TableColumn.SortType;

public class ColumnPreferences {

    public static final double DEFAULT_WIDTH = 100;
    public static final double ICON_COLUMN_WIDTH = 16 + 12; // add some additional space to improve appearance

    private final List<MainTableColumnModel> columns;
    private final boolean extraFileColumnsEnabled;
    private final Map<String, SortType> columnSortType;

    public ColumnPreferences(List<MainTableColumnModel> columns, boolean extraFileColumnsEnabled, Map<String, SortType> columnSortType) {
        this.columns = columns;
        this.extraFileColumnsEnabled = extraFileColumnsEnabled;
        this.columnSortType = columnSortType;
    }

    public boolean getExtraFileColumnsEnabled() { return extraFileColumnsEnabled; }

    public List<MainTableColumnModel> getColumns() {
        return columns;
    }

    public Map<String, SortType> getSortTypesForColumns() {
        return columnSortType;
    }
}
