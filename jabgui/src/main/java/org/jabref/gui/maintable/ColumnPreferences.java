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

    /// Creates object with default values
    private ColumnPreferences() {
        this(
                List.of(
                        new MainTableColumnModel(MainTableColumnModel.Type.GROUPS, "", 28),              // groups, default width: 28
                        new MainTableColumnModel(MainTableColumnModel.Type.GROUP_ICONS, "", 40),         // group_icons, default width: 40
                        new MainTableColumnModel(MainTableColumnModel.Type.FILES, "", 28),               // files, default width: 28
                        new MainTableColumnModel(MainTableColumnModel.Type.LINKED_IDENTIFIER, "", 28),   // linked_identifier, default width: 28
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "citationkey", 100),      // citationkey, default width: 100
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "entrytype", 75),         // entrytype, default width: 75
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "author/editor", 300),    // author/editor, default width: 300
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "title", 470),            // title, default width: 470
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "year", 60),              // year, default width: 60
                        new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, "journal/booktitle", 130), // journal, default width: 130
                        new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, "ranking", 50),          // ranking, default width: 50
                        new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, "readstatus", 50),       // readstatus, default width: 50
                        new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, "priority", 50)          // priority, default width: 50
                ),
                List.of()   // Empty list for additional column names
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
