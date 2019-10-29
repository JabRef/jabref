package org.jabref.gui.maintable;

import javafx.scene.control.TableColumn;

class MainTableColumn<T> extends TableColumn<BibEntryTableViewModel, T> {

    MainTableColumnType columnType;

    MainTableColumn(MainTableColumnType columnType) {
        this.columnType = columnType;
    }

    public MainTableColumnType getColumnType() { return columnType; }

}
