package org.jabref.gui.edit.automaticfiededitor.copyormovecontent;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorEvent;
import org.jabref.gui.edit.automaticfiededitor.MoveFieldValueAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

public class CopyOrMoveFieldContentTabViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    public static final int TAB_INDEX = 1;
    private final ObjectProperty<Field> fromField = new SimpleObjectProperty<>(StandardField.ABSTRACT);

    private final ObjectProperty<Field> toField = new SimpleObjectProperty<>(StandardField.AUTHOR);

    private final BooleanProperty overwriteFieldContent = new SimpleBooleanProperty(Boolean.FALSE);
    private final List<BibEntry> selectedEntries;
    private final NamedCompound dialogEdits;

    public CopyOrMoveFieldContentTabViewModel(List<BibEntry> selectedEntries, BibDatabase bibDatabase, NamedCompound dialogEdits) {
        super(bibDatabase);
        this.selectedEntries = new ArrayList<>(selectedEntries);
        this.dialogEdits = dialogEdits;
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
            dialogEdits.addEdit(copyFieldValueEdit);
        }
        eventBus.post(new AutomaticFieldEditorEvent(TAB_INDEX, affectedEntriesCount));
    }

    public void moveValue() {
        NamedCompound moveEdit = new NamedCompound("MOVE_EDIT");
        if (overwriteFieldContent.get()) {
            new MoveFieldValueAction(fromField.get(),
                                     toField.get(),
                                     selectedEntries,
                                     moveEdit).execute();

            if (moveEdit.hasEdits()) {
                moveEdit.end();
                dialogEdits.addEdit(moveEdit);
            }
        }
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
            dialogEdits.addEdit(swapFieldValuesEdit);
        }
        // TODO: Maybe pass the edits to the event bus and let the dialog register them?
        eventBus.post(new AutomaticFieldEditorEvent(TAB_INDEX, affectedEntriesCount));
    }

    public List<BibEntry> getSelectedEntries() {
        return selectedEntries;
    }
}
