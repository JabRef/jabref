package org.jabref.gui.maintable;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;

abstract class MainTableColumn<T> extends TableColumn<BibEntryTableViewModel, T> {

    MainTableColumn(String text) {
        super(text);

        setCellValueFactory(param -> getColumnValue(param.getValue()));
    }

    abstract ObservableValue<T> getColumnValue(BibEntryTableViewModel entry);
}
