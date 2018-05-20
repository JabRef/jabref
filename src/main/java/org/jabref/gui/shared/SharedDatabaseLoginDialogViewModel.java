package org.jabref.gui.shared;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.database.shared.DBMSType;

public class SharedDatabaseLoginDialogViewModel extends AbstractViewModel {

    private final ListProperty<DBMSType> allDBMSTypes = new SimpleListProperty<>(FXCollections.observableArrayList(DBMSType.values()));
    private final ObjectProperty<DBMSType> selectedDBMSType = new SimpleObjectProperty<>();

    public ListProperty<DBMSType> dbmstypeProperty() {
        return allDBMSTypes;
    }

    public ObjectProperty<DBMSType> selectedDbmstypeProperty() {
        return selectedDBMSType;
    }

}
