package org.jabref.gui.edit.automaticfiededitor.copyormovecontent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorUndoableEdit;
import org.jabref.gui.edit.automaticfiededitor.FieldHelper;
import org.jabref.gui.edit.automaticfiededitor.MoveFieldValueAction;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.jfxcore.validation.property.ConstrainedObjectProperty;
import org.jfxcore.validation.property.SimpleConstrainedObjectProperty;

public class CopyOrMoveFieldContentTabViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    private final ObjectProperty<Field> fromField = new SimpleObjectProperty<>(StandardField.ABSTRACT);

    private final ConstrainedObjectProperty<Field, ValidationMessage> toField;

    private final BooleanProperty overwriteFieldContent = new SimpleBooleanProperty(false);
    private final List<BibEntry> selectedEntries;

    private final BooleanBinding canMove;

    private final BooleanBinding canSwap;

    public CopyOrMoveFieldContentTabViewModel(BibDatabase bibDatabase,
                                              List<BibEntry> selectedEntries,
                                              NamedCompoundEdit compoundEdit,
                                              DialogService dialogService,
                                              StateManager stateManager) {
        super(bibDatabase, compoundEdit, dialogService, stateManager);
        this.selectedEntries = new ArrayList<>(selectedEntries);

        FieldHelper.getSetFieldsOnly(this.selectedEntries, getAllFields())
                   .stream()
                   .findFirst()
                   .ifPresent(fromField::set);

        toField = new SimpleConstrainedObjectProperty<Field, ValidationMessage>(StandardField.AUTHOR,
                ValidationConstraints.function(field -> {
                    if (StringUtil.isBlank(field.getName())) {
                        return Optional.of(ValidationMessage.error("Field name cannot be empty"));
                    } else if (StringUtil.containsWhitespace(field.getName())) {
                        return Optional.of(ValidationMessage.error("Field name cannot have whitespace characters"));
                    }
                    return Optional.empty();
                }));

        canMove = Bindings.and(toField.validProperty(), overwriteFieldContentProperty());

        canSwap = Bindings.and(toField.validProperty(), overwriteFieldContentProperty());
    }

    public BooleanBinding canMoveProperty() {
        return canMove;
    }

    public BooleanBinding canSwapProperty() {
        return canSwap;
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

    public ConstrainedObjectProperty<Field, ValidationMessage> toFieldProperty() {
        return toField;
    }

    public boolean isOverwriteFieldContent() {
        return overwriteFieldContent.get();
    }

    public BooleanProperty overwriteFieldContentProperty() {
        return overwriteFieldContent;
    }

    public void copyValue() {
        AutomaticFieldEditorUndoableEdit copyFieldValueEdit = new AutomaticFieldEditorUndoableEdit("COPY_FIELD_VALUE");
        int affectedEntriesCount = 0;
        for (BibEntry entry : selectedEntries) {
            String fromFieldValue = entry.getField(fromField.get()).orElse("");
            String toFieldValue = entry.getField(toField.get()).orElse("");

            if (overwriteFieldContent.get() || StringUtil.isBlank(toFieldValue)) {
                if (StringUtil.isNotBlank(fromFieldValue)) {
                    entry.setField(toField.get(), fromFieldValue);
                    copyFieldValueEdit.addEdit(new UndoableFieldChange(entry,
                            toField.get(),
                            toFieldValue,
                            fromFieldValue));
                    affectedEntriesCount++;
                }
            }
        }
        copyFieldValueEdit.setAffectedEntries(affectedEntriesCount);

        if (copyFieldValueEdit.hasEdits()) {
            copyFieldValueEdit.end();
        }

        addEdit(copyFieldValueEdit);
    }

    public void moveValue() {
        AutomaticFieldEditorUndoableEdit moveEdit = new AutomaticFieldEditorUndoableEdit("MOVE_EDIT");
        int affectedEntriesCount = 0;
        if (overwriteFieldContent.get()) {
            affectedEntriesCount = new MoveFieldValueAction(fromField.get(),
                    toField.get(),
                    selectedEntries,
                    moveEdit).executeAndGetAffectedEntriesCount();

            if (moveEdit.hasEdits()) {
                moveEdit.end();
            }
        }
        moveEdit.setAffectedEntries(affectedEntriesCount);

        addEdit(moveEdit);
    }

    public void swapValues() {
        AutomaticFieldEditorUndoableEdit swapFieldValuesEdit = new AutomaticFieldEditorUndoableEdit("SWAP_FIELD_VALUES");
        int affectedEntriesCount = 0;
        for (BibEntry entry : selectedEntries) {
            String fromFieldValue = entry.getField(fromField.get()).orElse("");
            String toFieldValue = entry.getField(toField.get()).orElse("");

            if (overwriteFieldContent.get() && StringUtil.isNotBlank(fromFieldValue) && StringUtil.isNotBlank(toFieldValue)) {
                entry.setField(toField.get(), fromFieldValue);
                entry.setField(fromField.get(), toFieldValue);

                swapFieldValuesEdit.addEdit(new UndoableFieldChange(
                        entry,
                        toField.get(),
                        toFieldValue,
                        fromFieldValue
                ));

                swapFieldValuesEdit.addEdit(new UndoableFieldChange(
                        entry,
                        fromField.get(),
                        fromFieldValue,
                        toFieldValue
                ));
                affectedEntriesCount++;
            }
        }
        swapFieldValuesEdit.setAffectedEntries(affectedEntriesCount);

        if (swapFieldValuesEdit.hasEdits()) {
            swapFieldValuesEdit.end();
        }

        addEdit(swapFieldValuesEdit);
    }
}
