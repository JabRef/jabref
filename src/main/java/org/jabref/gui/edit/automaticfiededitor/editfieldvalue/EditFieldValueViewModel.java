package org.jabref.gui.edit.automaticfiededitor.editfieldvalue;

import java.util.List;
import java.util.Optional;

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

    private final StringProperty selectedFieldName = new SimpleStringProperty();

    private final ObservableList<String> allFieldNames = FXCollections.observableArrayList();

    private final NamedCompound edits;

    public EditFieldValueViewModel(BibDatabaseContext databaseContext, List<BibEntry> selectedEntries, NamedCompound edits) {
        this.databaseContext = databaseContext;
        this.selectedEntries = selectedEntries;
        this.edits = edits;

        allFieldNames.addAll(databaseContext.getDatabase().getAllVisibleFields().stream().map(Field::getName).toList());
        selectedFieldName.setValue(allFieldNames.get(0));
    }

    public void clearSelectedField() {
        NamedCompound clearFieldEdit = new NamedCompound("");
        Field selectedField = parseField(selectedFieldName.get());

        for (BibEntry entry : selectedEntries) {
            Optional<String> oldFieldValue = entry.getField(selectedField);
            if (oldFieldValue.isPresent()) {
                entry.setField(selectedField, "");

                clearFieldEdit.addEdit(new UndoableFieldChange(entry,
                        selectedField,
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

    public ObservableList<String> getAllFieldNames() {
        return allFieldNames;
    }

    public StringProperty selectedFieldNameProperty() {
        return selectedFieldName;
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
