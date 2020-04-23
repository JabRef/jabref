package org.jabref.gui.maintable;

import java.util.List;

public class ColumnPreferences {

    public static final double DEFAULT_COLUMN_WIDTH = 100;
    public static final double ICON_COLUMN_WIDTH = 16 + 12; // add some additional space to improve appearance

    private final List<MainTableColumnModel> columns;
    private final List<MainTableColumnModel> columnSortOrder;

    public ColumnPreferences(List<MainTableColumnModel> columns, List<MainTableColumnModel> columnSortOrder) {
        this.columns = columns;
        this.columnSortOrder = columnSortOrder;
    }

    public List<MainTableColumnModel> getColumns() {
        return columns;
    }

    public List<MainTableColumnModel> getColumnSortOrder() {
        return columnSortOrder;
    }
}
