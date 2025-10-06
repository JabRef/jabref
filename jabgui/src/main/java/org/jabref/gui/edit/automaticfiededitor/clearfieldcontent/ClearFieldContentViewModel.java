package org.jabref.gui.edit.automaticfiededitor.clearfieldcontent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfiededitor.LastAutomaticFieldEditorEdit;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class ClearFieldContentViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    public static final int TAB_INDEX = 3;

    private final List<BibEntry> selectedEntries;

    private final StringProperty fieldValue = new SimpleStringProperty("");

    private final ObjectProperty<Field> selectedField = new SimpleObjectProperty<>(StandardField.AUTHOR);

    private final Validator fieldValidator;

    public ClearFieldContentViewModel(BibDatabase database, List<BibEntry> selectedEntries, StateManager stateManager) {
        super(database, stateManager);

        this.selectedEntries = new ArrayList<>(selectedEntries);
        this.selectedField.setValue(selectedEntries.getFirst().getFields().getFirst());

        fieldValidator = new FunctionBasedValidator<>(selectedField, field -> {
            if (StringUtil.isBlank(field.getName())) {
                return ValidationMessage.error("Field name cannot be empty");
            } else if (StringUtil.containsWhitespace(field.getName())) {
                return ValidationMessage.error("Field name cannot have whitespace characters");
            }
            return null;
        });
    }

    public ValidationStatus fieldValidationStatus() {
        return fieldValidator.getValidationStatus();
    }

    public void clearSelectedField() {
        NamedCompoundEdit clearFieldEdit = new NamedCompoundEdit("CLEAR_SELECTED_FIELD");
        int affectedEntriesCount = 0;
        for (BibEntry entry : selectedEntries) {
            Optional<String> oldFieldValue = entry.getField(selectedField.get());
            if (oldFieldValue.isPresent()) {
                entry.clearField(selectedField.get())
                     .ifPresent(fieldChange -> clearFieldEdit.addEdit(new UndoableFieldChange(fieldChange)));
                affectedEntriesCount++;
            }
        }

        if (clearFieldEdit.hasEdits()) {
            clearFieldEdit.end();
        }
        stateManager.setLastAutomaticFieldEditorEdit(new LastAutomaticFieldEditorEdit(
                affectedEntriesCount,
                TAB_INDEX,
                clearFieldEdit
        ));
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
}
