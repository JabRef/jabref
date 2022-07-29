package org.jabref.gui.edit.automaticfiededitor.copyormovecontent;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfiededitor.LastAutomaticFieldEditorEdit;
import org.jabref.gui.edit.automaticfiededitor.MoveFieldValueAction;
import org.jabref.gui.undo.NamedCompound;
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

public class CopyOrMoveFieldContentTabViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    public static final int TAB_INDEX = 1;
    private final ObjectProperty<Field> fromField = new SimpleObjectProperty<>(StandardField.ABSTRACT);

    private final ObjectProperty<Field> toField = new SimpleObjectProperty<>(StandardField.AUTHOR);

    private final BooleanProperty overwriteFieldContent = new SimpleBooleanProperty(Boolean.FALSE);
    private final List<BibEntry> selectedEntries;

    private final Validator toFieldValidator;

    private final BooleanBinding canMove;

    private final BooleanBinding canSwap;

    public CopyOrMoveFieldContentTabViewModel(List<BibEntry> selectedEntries, BibDatabase bibDatabase, StateManager stateManager) {
        super(bibDatabase, stateManager);
        this.selectedEntries = new ArrayList<>(selectedEntries);

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
        NamedCompound copyFieldValueEdit = new NamedCompound("COPY_FIELD_VALUE");
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
        if (copyFieldValueEdit.hasEdits()) {
            copyFieldValueEdit.end();
        }
        stateManager.setLastAutomaticFieldEditorEdit(new LastAutomaticFieldEditorEdit(
                affectedEntriesCount, TAB_INDEX, copyFieldValueEdit
        ));
    }

    public void moveValue() {
        NamedCompound moveEdit = new NamedCompound("MOVE_EDIT");
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
        stateManager.setLastAutomaticFieldEditorEdit(new LastAutomaticFieldEditorEdit(
                affectedEntriesCount, TAB_INDEX, moveEdit
        ));
    }

    public void swapValues() {
        NamedCompound swapFieldValuesEdit = new NamedCompound("SWAP_FIELD_VALUES");
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

        if (swapFieldValuesEdit.hasEdits()) {
            swapFieldValuesEdit.end();
        }
        stateManager.setLastAutomaticFieldEditorEdit(new LastAutomaticFieldEditorEdit(
                affectedEntriesCount, TAB_INDEX, swapFieldValuesEdit
        ));
    }

    public List<BibEntry> getSelectedEntries() {
        return selectedEntries;
    }
}
