package org.jabref.gui.edit.automaticfiededitor.clearcontent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorUndoableEdit;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class ClearContentViewModel extends AbstractAutomaticFieldEditorTabViewModel {
    private final List<BibEntry> selectedEntries;

    public ClearContentViewModel(BibDatabase bibDatabase,
                                 List<BibEntry> selectedEntries,
                                 NamedCompoundEdit compoundEdit,
                                 DialogService dialogService,
                                 StateManager stateManager) {
        super(bibDatabase, compoundEdit, dialogService, stateManager);
        this.selectedEntries = new ArrayList<>(selectedEntries);
    }

    public ObservableList<Field> getAllFields() {
        return FXCollections.observableArrayList(FieldFactory.getAllFieldsWithOutInternal());
    }

    public void clearField(Field field) {
        AutomaticFieldEditorUndoableEdit edits = new AutomaticFieldEditorUndoableEdit("CLEAR_SELECTED_FIELD");
        int affectedEntriesCount = 0;
        for (BibEntry entry : this.selectedEntries) {
            Optional<String> oldFieldValue = entry.getField(field);
            if (oldFieldValue.isPresent()) {
                entry.clearField(field)
                     .ifPresent(change -> edits.addEdit(new UndoableFieldChange(change)));
                affectedEntriesCount++;
            }
        }
        edits.setAffectedEntries(affectedEntriesCount);

        if (edits.hasEdits()) {
            edits.end();
        }

        addEdit(edits);
    }
}
