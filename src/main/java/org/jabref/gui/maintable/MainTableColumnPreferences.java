package org.jabref.gui.maintable;

import java.util.List;

public class MainTableColumnPreferences implements AbstractColumnPreferences {

    public static final double DEFAULT_COLUMN_WIDTH = 100;
    public static final double ICON_COLUMN_WIDTH = 16 + 12; // add some additional space to improve appearance

    private final List<MainTableColumnModel> columns;
    private final List<MainTableColumnModel> columnSortOrder;

    public MainTableColumnPreferences(List<MainTableColumnModel> columns, List<MainTableColumnModel> columnSortOrder) {
        this.columns = columns;
        this.columnSortOrder = columnSortOrder;
    }

    @Override
    public List<MainTableColumnModel> getColumns() {
        return columns;
    }

    @Override
    public List<MainTableColumnModel> getColumnSortOrder() {
        return columnSortOrder;
    }
}
