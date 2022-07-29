package org.jabref.gui.edit.automaticfiededitor.renamefield;

import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfiededitor.LastAutomaticFieldEditorEdit;
import org.jabref.gui.edit.automaticfiededitor.MoveFieldValueAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class RenameFieldViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    public static final int TAB_INDEX = 2;
    private final StringProperty newFieldName = new SimpleStringProperty("");
    private final ObjectProperty<Field> selectedField = new SimpleObjectProperty<>(StandardField.AUTHOR);
    private final List<BibEntry> selectedEntries;

    private final Validator fieldValidator;

    private final Validator fieldNameValidator;

    private final BooleanBinding canRename;

    public RenameFieldViewModel(List<BibEntry> selectedEntries, BibDatabase database, StateManager stateManager) {
        super(database, stateManager);
        this.selectedEntries = selectedEntries;

        fieldValidator = new FunctionBasedValidator<>(selectedField, field -> StringUtil.isNotBlank(field.getName()),
                ValidationMessage.error("Field cannot be empty"));
        fieldNameValidator = new FunctionBasedValidator<>(newFieldName, fieldName -> {
            if (StringUtil.isBlank(fieldName)) {
                return ValidationMessage.error("Field name cannot be empty");
            } else if (StringUtil.containsWhitespace(fieldName)) {
                return ValidationMessage.error("Field name cannot have whitespace characters");
            }
            return null;
        });

        canRename = Bindings.and(fieldValidationStatus().validProperty(), fieldNameValidationStatus().validProperty());
    }

    public ValidationStatus fieldValidationStatus() {
        return fieldValidator.getValidationStatus();
    }

    public ValidationStatus fieldNameValidationStatus() {
        return fieldNameValidator.getValidationStatus();
    }

    public BooleanBinding canRenameProperty() {
        return canRename;
    }

    public String getNewFieldName() {
        return newFieldName.get();
    }

    public StringProperty newFieldNameProperty() {
        return newFieldName;
    }

    public void setNewFieldName(String newName) {
        newFieldNameProperty().set(newName);
    }

    public Field getSelectedField() {
        return selectedField.get();
    }

    public ObjectProperty<Field> selectedFieldProperty() {
        return selectedField;
    }

    public void selectField(Field field) {
        selectedFieldProperty().set(field);
    }

    public void renameField() {
        NamedCompound renameEdit = new NamedCompound("RENAME_EDIT");
        int affectedEntriesCount = 0;
        if (fieldNameValidationStatus().isValid()) {
            affectedEntriesCount = new MoveFieldValueAction(selectedField.get(),
                    FieldFactory.parseField(newFieldName.get()),
                    selectedEntries,
                    renameEdit,
                    false).executeAndGetAffectedEntriesCount();

            if (renameEdit.hasEdits()) {
                renameEdit.end();
            }
        }

        stateManager.setLastAutomaticFieldEditorEdit(new LastAutomaticFieldEditorEdit(
                affectedEntriesCount, TAB_INDEX, renameEdit
        ));
    }
}
