package org.jabref.gui.edit.automaticfiededitor.copyormovecontent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.edit.automaticfiededitor.MoveFieldValueAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

public class CopyOrMoveFieldContentTabViewModel extends AbstractViewModel {
    private final ObjectProperty<Field> fromField = new SimpleObjectProperty<>();

    private final ObjectProperty<Field> toField = new SimpleObjectProperty<>();

    private final BooleanProperty overwriteFieldContent = new SimpleBooleanProperty();
    private final ObservableList<Field> allFields = FXCollections.observableArrayList();
    // TODO: Create an abstraction where selectedEntries, databaseContext and dialogEdits dependencies are shared across
    //  all automatic field editors tab view models
    private final List<BibEntry> selectedEntries;
    private final NamedCompound dialogEdits;

    public CopyOrMoveFieldContentTabViewModel(List<BibEntry> selectedEntries, Set<Field> visibleFields, NamedCompound dialogEdits) {
        this.selectedEntries = new ArrayList<>(selectedEntries);
        this.dialogEdits = dialogEdits;

        allFields.addAll(visibleFields);
    }

    public ObservableList<Field> getAllFields() {
        return allFields;
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

        for (BibEntry entry : selectedEntries) {
            String fromFieldValue = entry.getField(fromField.get()).orElse("");
            String toFieldValue = entry.getField(toField.get()).orElse("");

            if (overwriteFieldContent.get() || StringUtil.isBlank(toFieldValue)) {
                if (StringUtil.isNotBlank(fromFieldValue)) {
                    entry.setField(toField.get(), fromFieldValue);
                }

                copyFieldValueEdit.addEdit(new UndoableFieldChange(entry,
                                                                   toField.get(),
                                                                   toFieldValue,
                                                                   fromFieldValue));
            }
        }

        if (copyFieldValueEdit.hasEdits()) {
            copyFieldValueEdit.end();
            dialogEdits.addEdit(copyFieldValueEdit);
        }
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
            }
        }

        if (swapFieldValuesEdit.hasEdits()) {
            swapFieldValuesEdit.end();
            dialogEdits.addEdit(swapFieldValuesEdit);
        }
    }

    public List<BibEntry> getSelectedEntries() {
        return selectedEntries;
    }
}
