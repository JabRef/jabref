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

    // Private default constructor with hardcoded default values
    private ColumnPreferences() {
        this(
                List.of(
                        new MainTableColumnModel(MainTableColumnModel.Type.GROUPS, "", 28),
                        new MainTableColumnModel(MainTableColumnModel.Type.GROUP_ICONS, "", 40),
                        new MainTableColumnModel(MainTableColumnModel.Type.FILES, "", 28),
                        new MainTableColumnModel(MainTableColumnModel.Type.LINKED_IDENTIFIER, "", 28),
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "citationkey", 100),
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "entrytype", 75),
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "author/editor", 300),
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "title", 470),
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "year", 60),
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "journal/booktitle", 130),
                        new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, "ranking", 50),
                        new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, "readstatus", 50),
                        new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, "priority", 50)
                ),
                List.of()
        );
    }

    public static ColumnPreferences getDefault() {
        return new ColumnPreferences();
    }

    public void setAll(ColumnPreferences preferences) {
        this.columns.setAll(preferences.getColumns());
        this.columnSortOrder.setAll(preferences.getColumnSortOrder());
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
