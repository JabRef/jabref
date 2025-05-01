package org.jabref.gui.maintable;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ColumnPreferences {

    public static final double DEFAULT_COLUMN_WIDTH = 100;
    public static final double ICON_COLUMN_WIDTH = 16 + 12; // add some additional space to improve appearance

    private final ObservableList<MainTableColumnModel> columns;
    private final ObservableList<MainTableColumnModel> columnSortOrder;

    public ColumnPreferences(List<MainTableColumnModel> columns,
                             List<MainTableColumnModel> columnSortOrder) {
        this.columns = FXCollections.observableArrayList(columns);
        this.columnSortOrder = FXCollections.observableArrayList(columnSortOrder);
    }

    public ObservableList<MainTableColumnModel> getColumns() {
        return columns;
    }

    public ObservableList<MainTableColumnModel> getColumnSortOrder() {
        return columnSortOrder;
    }

    public void setColumns(List<MainTableColumnModel> list) {
        columns.clear();
        columns.addAll(list);
    }

    public void setColumnSortOrder(List<MainTableColumnModel> list) {
        columnSortOrder.clear();
        columnSortOrder.addAll(list);
    }
}
