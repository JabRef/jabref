package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ColumnPreferences {

   
    public static final String DEFAULT_COLUMN_NAMES = "groups;group_icons;files;linked_id;field:citationkey;field:entrytype;field:author/editor;field:title;field:year;field:journal/booktitle;special:ranking;special:readstatus;special:priority";
    public static final String DEFAULT_COLUMN_WIDTHS = "28;40;28;28;100;75;300;470;60;130;50;50;50";

    public static final double DEFAULT_COLUMN_WIDTH = 100;
    public static final double ICON_COLUMN_WIDTH = 16 + 12;

    private final ObservableList<MainTableColumnModel> columns;
    private final ObservableList<MainTableColumnModel> columnSortOrder;

    public ColumnPreferences(List<MainTableColumnModel> columns,
                             List<MainTableColumnModel> columnSortOrder) {
        this.columns = FXCollections.observableArrayList(columns);
        this.columnSortOrder = FXCollections.observableArrayList(columnSortOrder);
    }

   
    public static ColumnPreferences getDefault() {
        List<String> names = Arrays.asList(DEFAULT_COLUMN_NAMES.split(";"));
        List<Double> widths = Arrays.stream(DEFAULT_COLUMN_WIDTHS.split(";"))
                                    .map(Double::parseDouble)
                                    .collect(Collectors.toList());

        List<MainTableColumnModel> defaultColumns = new ArrayList<>();
        
        for (int i = 0; i < names.size(); i++) {
            MainTableColumnModel model = MainTableColumnModel.parse(names.get(i));
            
            if (i < widths.size()) {
                model.widthProperty().setValue(widths.get(i));
            }
            
            defaultColumns.add(model);
        }

        return new ColumnPreferences(defaultColumns, Collections.emptyList());
    }

    public void setAll(ColumnPreferences other) {
        this.columns.setAll(other.getColumns());
        this.columnSortOrder.setAll(other.getColumnSortOrder());
    }

    // --- Existing Getters/Setters ---

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