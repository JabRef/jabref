package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;

public class ColumnPreferences {

    public static final double DEFAULT_COLUMN_WIDTH = 100;
    public static final double ICON_COLUMN_WIDTH = 16 + 12; // add some additional space to improve appearance

    private List<MainTableColumnModel> columns;
    private List<MainTableColumnModel> columnSortOrder = new ArrayList<>();

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

    public void setColumns(List<MainTableColumnModel> columns) {
        this.columns = columns;
    }

    public void setColumnSorOrder(List<MainTableColumnModel> columnSortOrder) {
        this.columnSortOrder = columnSortOrder;
    }

    @Override
    public String toString() {
        return "ColumnPreferences [columns=" + columns + ", columnSortOrder=" + columnSortOrder + "]";
    }
}
