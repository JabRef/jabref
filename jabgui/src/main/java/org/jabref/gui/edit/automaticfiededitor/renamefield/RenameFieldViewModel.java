package org.jabref.gui.edit.automaticfiededitor.renamefield;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorUndoableEdit;
import org.jabref.gui.edit.automaticfiededitor.FieldHelper;
import org.jabref.gui.edit.automaticfiededitor.MoveFieldValueAction;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.jfxcore.validation.property.ConstrainedObjectProperty;
import org.jfxcore.validation.property.ConstrainedStringProperty;
import org.jfxcore.validation.property.SimpleConstrainedObjectProperty;
import org.jfxcore.validation.property.SimpleConstrainedStringProperty;

public class RenameFieldViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    private final ConstrainedStringProperty<ValidationMessage> newFieldName;
    private final ConstrainedObjectProperty<Field, ValidationMessage> selectedField;
    private final List<BibEntry> selectedEntries;

    private final BooleanBinding canRename;

    public RenameFieldViewModel(List<BibEntry> selectedEntries,
                                BibDatabase database,
                                NamedCompoundEdit compoundEdit,
                                DialogService dialogService,
                                StateManager stateManager) {
        super(database, compoundEdit, dialogService, stateManager);
        this.selectedEntries = new ArrayList<>(selectedEntries);

        selectedField = new SimpleConstrainedObjectProperty<Field, ValidationMessage>(StandardField.AUTHOR,
                ValidationConstraints.predicate(field -> StringUtil.isNotBlank(field.getName()),
                        ValidationMessage.error("Field cannot be empty")));
        FieldHelper.getSetFieldsOnly(selectedEntries, getAllFields())
                   .stream().findFirst().ifPresent(selectedField::set);

        newFieldName = new SimpleConstrainedStringProperty<>("",
                ValidationConstraints.function(fieldName -> {
                    if (StringUtil.isBlank(fieldName)) {
                        return Optional.of(ValidationMessage.error("Field name cannot be empty"));
                    } else if (StringUtil.containsWhitespace(fieldName)) {
                        return Optional.of(ValidationMessage.error("Field name cannot have whitespace characters"));
                    }
                    return Optional.empty();
                }));

        canRename = Bindings.and(selectedField.validProperty(), newFieldName.validProperty());
    }

    public BooleanBinding canRenameProperty() {
        return canRename;
    }

    public String getNewFieldName() {
        return newFieldName.get();
    }

    public ConstrainedStringProperty<ValidationMessage> newFieldNameProperty() {
        return newFieldName;
    }

    public void setNewFieldName(String newName) {
        newFieldNameProperty().set(newName);
    }

    public Field getSelectedField() {
        return selectedField.get();
    }

    public ConstrainedObjectProperty<Field, ValidationMessage> selectedFieldProperty() {
        return selectedField;
    }

    public void selectField(Field field) {
        selectedFieldProperty().set(field);
    }

    public void renameField() {
        AutomaticFieldEditorUndoableEdit edits = new AutomaticFieldEditorUndoableEdit("RENAME_EDIT");
        int affectedEntriesCount = 0;
        if (newFieldName.isValid()) {
            affectedEntriesCount = new MoveFieldValueAction(selectedField.get(),
                    FieldFactory.parseField(newFieldName.get()),
                    selectedEntries,
                    edits,
                    false).executeAndGetAffectedEntriesCount();

            edits.setAffectedEntries(affectedEntriesCount);

            if (edits.hasEdits()) {
                edits.end();
            }
        }

        addEdit(edits);
    }
}
