package org.jabref.gui.edit.automaticfiededitor.copyormovecontent;

import java.util.ArrayList;
import java.util.List;

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
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class CopyOrMoveFieldContentTabViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    private final ObjectProperty<Field> fromField = new SimpleObjectProperty<>(StandardField.ABSTRACT);

    private final ObjectProperty<Field> toField = new SimpleObjectProperty<>(StandardField.AUTHOR);

    private final BooleanProperty overwriteFieldContent = new SimpleBooleanProperty(false);
    private final List<BibEntry> selectedEntries;

    private final Validator toFieldValidator;

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

        toFieldValidator = new FunctionBasedValidator<>(toField, field -> {
            if (StringUtil.isBlank(field.getName())) {
                return ValidationMessage.error("Field name cannot be empty");
            } else if (StringUtil.containsWhitespace(field.getName())) {
                return ValidationMessage.error("Field name cannot have whitespace characters");
            }
            return null;
        });

        canMove = BooleanBinding.booleanExpression(toFieldValidationStatus().validProperty())
                                .and(overwriteFieldContentProperty());

        canSwap = BooleanBinding.booleanExpression(toFieldValidationStatus().validProperty())
                                .and(overwriteFieldContentProperty());
    }

    public ValidationStatus toFieldValidationStatus() {
        return toFieldValidator.getValidationStatus();
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

    public ObjectProperty<Field> toFieldProperty() {
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
