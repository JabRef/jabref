package org.jabref.gui.edit.automaticfiededitor.editfieldcontent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorUndoableEdit;
import org.jabref.gui.edit.automaticfiededitor.FieldHelper;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class EditFieldContentViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    private final List<BibEntry> selectedEntries;

    private final StringProperty fieldValue = new SimpleStringProperty("");

    private final ObjectProperty<Field> selectedField = new SimpleObjectProperty<>(StandardField.AUTHOR);

    private final BooleanProperty overwriteFieldContent = new SimpleBooleanProperty(Boolean.FALSE);

    private final Validator fieldValidator;
    private final BooleanBinding canAppend;

    public EditFieldContentViewModel(BibDatabase database,
                                     List<BibEntry> selectedEntries,
                                     NamedCompoundEdit compoundEdit,
                                     DialogService dialogService,
                                     StateManager stateManager) {
        super(database, compoundEdit, dialogService, stateManager);
        this.selectedEntries = new ArrayList<>(selectedEntries);

        FieldHelper.getSetFieldsOnly(selectedEntries, getAllFields())
                   .stream().findFirst().ifPresent(selectedField::set);

        fieldValidator = new FunctionBasedValidator<>(selectedField, field -> {
            if (StringUtil.isBlank(field.getName())) {
                return ValidationMessage.error("Field name cannot be empty");
            } else if (StringUtil.containsWhitespace(field.getName())) {
                return ValidationMessage.error("Field name cannot have whitespace characters");
            }
            return null;
        });

        canAppend = Bindings.and(overwriteFieldContentProperty(), fieldValidationStatus().validProperty());
    }

    public ValidationStatus fieldValidationStatus() {
        return fieldValidator.getValidationStatus();
    }

    public BooleanBinding canAppendProperty() {
        return canAppend;
    }

    public void setFieldValue() {
        AutomaticFieldEditorUndoableEdit edits = new AutomaticFieldEditorUndoableEdit("CHANGE_SELECTED_FIELD");
        String toSetFieldValue = fieldValue.getValue();
        int affectedEntriesCount = 0;
        for (BibEntry entry : selectedEntries) {
            Optional<String> oldFieldValue = entry.getField(selectedField.get());
            if (oldFieldValue.isEmpty() || overwriteFieldContent.get()) {
                entry.setField(selectedField.get(), toSetFieldValue)
                     .ifPresent(fieldChange -> edits.addEdit(new UndoableFieldChange(fieldChange)));
                fieldValue.set("");
                // TODO: increment affected entries only when UndoableFieldChange.isPresent()
                affectedEntriesCount++;
            }
        }
        edits.setAffectedEntries(affectedEntriesCount);

        if (edits.hasEdits()) {
            edits.end();
        }

        addEdit(edits);
    }

    public void appendToFieldValue() {
        AutomaticFieldEditorUndoableEdit edits = new AutomaticFieldEditorUndoableEdit("APPEND_TO_SELECTED_FIELD");
        String toAppendFieldValue = fieldValue.getValue();
        int affectedEntriesCount = 0;
        for (BibEntry entry : selectedEntries) {
            Optional<String> oldFieldValue = entry.getField(selectedField.get());
            // Append button should be disabled if 'overwriteNonEmptyFields' is false
            if (overwriteFieldContent.get()) {
                String newFieldValue = oldFieldValue.orElse("").concat(toAppendFieldValue);

                entry.setField(selectedField.get(), newFieldValue)
                     .ifPresent(fieldChange -> edits.addEdit(new UndoableFieldChange(fieldChange)));

                fieldValue.set("");
                affectedEntriesCount++;
            }
        }
        edits.setAffectedEntries(affectedEntriesCount);

        if (edits.hasEdits()) {
            edits.end();
        }

        addEdit(edits);
    }

    public ObjectProperty<Field> selectedFieldProperty() {
        return selectedField;
    }

    public Field getSelectedField() {
        return selectedFieldProperty().get();
    }

    public StringProperty fieldValueProperty() {
        return fieldValue;
    }

    public BooleanProperty overwriteFieldContentProperty() {
        return overwriteFieldContent;
    }
}
