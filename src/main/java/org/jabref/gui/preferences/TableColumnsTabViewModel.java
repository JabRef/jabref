package org.jabref.gui.preferences;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.entry.specialfields.SpecialField;

public class TableColumnsTabViewModel {

    private final StringProperty enteredNameProperty = new SimpleStringProperty("");
    private final ObservableList<SpecialField> specialFieldColumns = FXCollections.emptyObservableList();

    public TableColumnsTabViewModel() {

    }

    public void addNewColumn() {
        // TODO Auto-generated method stub

    }

    public ObservableList<SpecialField> specialFieldColumnsProperty() {
        return specialFieldColumns;
    }

    public StringProperty enteredNameProperty() {
        return enteredNameProperty;
    }
}
