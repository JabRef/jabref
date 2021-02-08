package org.jabref.gui.maintable;

import java.util.List;

public class ColumnPreferences {

    public static final double DEFAULT_COLUMN_MIN_WIDTH = 80;
    public static final double DEFAULT_COLUMN_WIDTH = 100;
    public static final double ICON_COLUMN_WIDTH = 16 + 12; // add some additional space to improve appearance

    private final List<MainTableColumnModel> columns;

    private final List<MainTableColumnModel> columnSortOrder;

    private final boolean dedicatedFileColumnsEnabled;

    public ColumnPreferences(List<MainTableColumnModel> columns, List<MainTableColumnModel> columnSortOrder, boolean dedicatedFileColumnsEnabled) {
        this.columns = columns;
        this.columnSortOrder = columnSortOrder;
        this.dedicatedFileColumnsEnabled = dedicatedFileColumnsEnabled;
    }

    public List<MainTableColumnModel> getColumns() {
        return columns;
    }

    public List<MainTableColumnModel> getColumnSortOrder() {
        return columnSortOrder;
    }

    public boolean isDedicatedFileColumnsEnabled() {
        return dedicatedFileColumnsEnabled;
    }

}
