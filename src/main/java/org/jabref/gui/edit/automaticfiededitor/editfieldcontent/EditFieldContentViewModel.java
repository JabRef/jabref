package org.jabref.gui.edit.automaticfiededitor.editfieldcontent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

public class EditFieldContentViewModel extends AbstractViewModel {
    private final BibDatabaseContext databaseContext;
    private final List<BibEntry> selectedEntries;

    private final StringProperty fieldValue = new SimpleStringProperty();

    private final ObjectProperty<Field> selectedField = new SimpleObjectProperty<>();

    private final BooleanProperty overwriteNonEmptyFields = new SimpleBooleanProperty();

    private final ObservableList<Field> allFields = FXCollections.observableArrayList();

    private final NamedCompound dialogEdits;

    public EditFieldContentViewModel(BibDatabaseContext databaseContext, List<BibEntry> selectedEntries, NamedCompound dialogEdits) {
        this.databaseContext = databaseContext;
        this.selectedEntries = new ArrayList<>(selectedEntries);
        this.dialogEdits = dialogEdits;

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
            dialogEdits.addEdit(clearFieldEdit);
        }
    }

    public void setFieldValue() {
        NamedCompound setFieldEdit = new NamedCompound("CHANGE_SELECTED_FIELD");
        String toSetFieldValue = fieldValue.getValue();

        for (BibEntry entry : selectedEntries) {
            Optional<String> oldFieldValue = entry.getField(selectedField.get());
            if (oldFieldValue.isEmpty() || overwriteNonEmptyFields.get()) {
                entry.setField(selectedField.get(), toSetFieldValue);

                setFieldEdit.addEdit(new UndoableFieldChange(entry,
                        selectedField.get(),
                        oldFieldValue.orElse(null),
                        toSetFieldValue));
                fieldValue.set("");
            }
        }

        if (setFieldEdit.hasEdits()) {
            setFieldEdit.end();
            dialogEdits.addEdit(setFieldEdit);
        }
    }

    public void appendToFieldValue() {
        NamedCompound appendToFieldEdit = new NamedCompound("APPEND_TO_SELECTED_FIELD");
        String toAppendFieldValue = fieldValue.getValue();

        for (BibEntry entry : selectedEntries) {
            Optional<String> oldFieldValue = entry.getField(selectedField.get());
            // Append button should be disabled if 'overwriteNonEmptyFields' is false
            if (overwriteNonEmptyFields.get()) {
                String newFieldValue = oldFieldValue.orElse("").concat(toAppendFieldValue);

                entry.setField(selectedField.get(), newFieldValue);
                appendToFieldEdit.addEdit(new UndoableFieldChange(entry,
                        selectedField.get(),
                        oldFieldValue.orElse(null),
                        newFieldValue)
                );

                fieldValue.set("");
            }
        }

        if (appendToFieldEdit.hasEdits()) {
            appendToFieldEdit.end();
            dialogEdits.addEdit(appendToFieldEdit);
        }
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

    public BooleanProperty overwriteNonEmptyFieldsProperty() {
        return overwriteNonEmptyFields;
    }
}
