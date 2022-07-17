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

import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class EditFieldContentViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    private final List<BibEntry> selectedEntries;

    private final StringProperty fieldValue = new SimpleStringProperty("");

    private final ObjectProperty<Field> selectedField = new SimpleObjectProperty<>(StandardField.AUTHOR);

    private final BooleanProperty overwriteFieldContent = new SimpleBooleanProperty(Boolean.FALSE);

    private final NamedCompound dialogEdits;

    public EditFieldContentViewModel(BibDatabase database, List<BibEntry> selectedEntries, NamedCompound dialogEdits) {
        super(database);
        this.selectedEntries = new ArrayList<>(selectedEntries);
        this.dialogEdits = dialogEdits;
    }

    public void clearSelectedField() {
        NamedCompound clearFieldEdit = new NamedCompound("CLEAR_SELECTED_FIELD");

        for (BibEntry entry : selectedEntries) {
            Optional<String> oldFieldValue = entry.getField(selectedField.get());
            if (oldFieldValue.isPresent()) {
                entry.clearField(selectedField.get())
                        .ifPresent(fieldChange -> clearFieldEdit.addEdit(new UndoableFieldChange(fieldChange)));
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
            if (oldFieldValue.isEmpty() || overwriteFieldContent.get()) {
                entry.setField(selectedField.get(), toSetFieldValue)
                        .ifPresent(fieldChange -> setFieldEdit.addEdit(new UndoableFieldChange(fieldChange)));
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
            if (overwriteFieldContent.get()) {
                String newFieldValue = oldFieldValue.orElse("").concat(toAppendFieldValue);

                entry.setField(selectedField.get(), newFieldValue)
                        .ifPresent(fieldChange -> appendToFieldEdit.addEdit(new UndoableFieldChange(fieldChange)));

                fieldValue.set("");
            }
        }

        if (appendToFieldEdit.hasEdits()) {
            appendToFieldEdit.end();
            dialogEdits.addEdit(appendToFieldEdit);
        }
    }

    public ObjectProperty<Field> selectedFieldProperty() {
        return selectedField;
    }

    public Field getSelectedField() {
        return selectedFieldProperty().get();
    }

    public String getFieldValue() {
        return fieldValue.get();
    }

    public StringProperty fieldValueProperty() {
        return fieldValue;
    }

    public BooleanProperty overwriteFieldContentProperty() {
        return overwriteFieldContent;
    }
}
