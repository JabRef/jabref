package org.jabref.gui.edit.automaticfiededitor.twofields;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class TwoFieldsViewModel extends AbstractViewModel {
    private final ObjectProperty<Field> fromField = new SimpleObjectProperty<>();

    private final ObjectProperty<Field> toField = new SimpleObjectProperty<>();

    private final BooleanProperty overwriteNonEmptyFields = new SimpleBooleanProperty();
    private final ObservableList<Field> allFields = FXCollections.observableArrayList();
    // TODO: Create an abstraction where selectedEntries, databaseContext and dialogEdits dependencies are shared across
    //  all automatic field editors tab view models
    private final List<BibEntry> selectedEntries;
    private final BibDatabaseContext databaseContext;
    private final NamedCompound dialogEdits;

    public TwoFieldsViewModel(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext, NamedCompound dialogEdits) {
        this.selectedEntries = selectedEntries;
        this.databaseContext = databaseContext;
        this.dialogEdits = dialogEdits;

        allFields.addAll(databaseContext.getDatabase().getAllVisibleFields());
    }

    public ObservableList<Field> getAllFields() {
        return allFields;
    }

    public Field getFromField() {
        return fromField.get();
    }

    public ObjectProperty<Field> fromFieldProperty() {
        return fromField;
    }

    public Field getToField() {
        return toField.get();
    }

    public ObjectProperty<Field> toFieldProperty() {
        return toField;
    }

    public boolean getOverwriteNonEmptyFields() {
        return overwriteNonEmptyFields.get();
    }

    public BooleanProperty overwriteNonEmptyFieldsProperty() {
        return overwriteNonEmptyFields;
    }

    public void copyValue() {
    }

    public void moveValue() {
    }

    public void swapValues() {
    }
}
