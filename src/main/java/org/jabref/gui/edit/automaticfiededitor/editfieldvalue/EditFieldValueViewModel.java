package org.jabref.gui.edit.automaticfiededitor.editfieldvalue;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class EditFieldValueViewModel extends AbstractViewModel {
    private final BibDatabaseContext databaseContext;
    private final List<BibEntry> selectedEntries;

    private final StringProperty fieldValue = new SimpleStringProperty();

    private final ObjectProperty<Field> selectedField = new SimpleObjectProperty<>();

    private final ObservableList<Field> allFields = FXCollections.observableArrayList();

    private final NamedCompound edits;

    public EditFieldValueViewModel(BibDatabaseContext databaseContext, List<BibEntry> selectedEntries, NamedCompound edits) {
        this.databaseContext = databaseContext;
        this.selectedEntries = selectedEntries;
        this.edits = edits;

        allFields.addAll(databaseContext.getDatabase().getAllVisibleFields().stream().toList());
    }

    public void clearSelectedField() {
        NamedCompound clearFieldEdit = new NamedCompound("CLEAR_SELECTED_FIELD");

        for (BibEntry entry : selectedEntries) {
            Optional<String> oldFieldValue = entry.getField(selectedField.get());
            if (oldFieldValue.isPresent()) {
                entry.setField(selectedField.get(), "");

                clearFieldEdit.addEdit(new UndoableFieldChange(entry,
                        selectedField.get(),
                        oldFieldValue.orElse(null),
                        fieldValue.get()));
            }
        }

        if (clearFieldEdit.hasEdits()) {
            clearFieldEdit.end();
            edits.addEdit(clearFieldEdit);
        }
    }

    public void setFieldValue() {
    }

    public void appendToFieldValue() {
    }

    public ObservableList<Field> getAllFields() {
        return allFields;
    }

    public ObjectProperty<Field> selectedFieldProperty() {
        return selectedField;
    }

    public String getFieldValue() {
        return fieldValue.get();
    }

    public StringProperty fieldValueProperty() {
        return fieldValue;
    }

    Field parseField(String name) {
        return FieldFactory.parseField(name);
    }
}
