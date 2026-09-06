package org.jabref.gui.edit.automaticfieldeditor.copyormovecontent;

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
import org.jabref.gui.edit.automaticfieldeditor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfieldeditor.FieldHelper;
import org.jabref.gui.edit.automaticfieldeditor.MoveFieldValueAction;
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableFieldChange;

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
                                              CompoundEdit compoundEdit,
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
                    if (StringUtil.isBlank(field.getName()) || StringUtil.containsWhitespace(field.getName())) {
                        return Optional.of(ValidationMessage.error(Localization.lang("Field cannot be empty and must not contain spaces.")));
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
        CompoundEdit copyFieldValueEdit = new CompoundEdit(Localization.lang("Copy content"));
        int affectedEntriesCount = 0;
        for (BibEntry entry : selectedEntries) {
            String fromFieldValue = entry.getField(fromField.get()).orElse("");
            String toFieldValue = entry.getField(toField.get()).orElse("");

            if (overwriteFieldContent.get() || StringUtil.isBlank(toFieldValue)) {
                if (StringUtil.isNotBlank(fromFieldValue)) {
                    copyFieldValueEdit.applyEdit(new UndoableFieldChange(entry, toField.get(), toFieldValue, fromFieldValue));
                    affectedEntriesCount++;
                }
            }
        }

        addEdit(copyFieldValueEdit, affectedEntriesCount);
    }

    public void moveValue() {
        CompoundEdit moveEdit = new CompoundEdit(Localization.lang("Move content"));
        int affectedEntriesCount = 0;
        if (overwriteFieldContent.get()) {
            affectedEntriesCount = new MoveFieldValueAction(fromField.get(),
                    toField.get(),
                    selectedEntries,
                    moveEdit).executeAndGetAffectedEntriesCount();
        }

        addEdit(moveEdit, affectedEntriesCount);
    }

    public void swapValues() {
        CompoundEdit swapFieldValuesEdit = new CompoundEdit(Localization.lang("Swap content"));
        int affectedEntriesCount = 0;
        for (BibEntry entry : selectedEntries) {
            String fromFieldValue = entry.getField(fromField.get()).orElse("");
            String toFieldValue = entry.getField(toField.get()).orElse("");

            if (overwriteFieldContent.get() && StringUtil.isNotBlank(fromFieldValue) && StringUtil.isNotBlank(toFieldValue)) {
                swapFieldValuesEdit.applyEdit(new UndoableFieldChange(entry, toField.get(), toFieldValue, fromFieldValue));
                swapFieldValuesEdit.applyEdit(new UndoableFieldChange(entry, fromField.get(), fromFieldValue, toFieldValue));
                affectedEntriesCount++;
            }
        }

        addEdit(swapFieldValuesEdit, affectedEntriesCount);
    }
}
