package org.jabref.gui.edit.automaticfiededitor.clearcontent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.LastAutomaticFieldEditorEdit;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class ClearContentViewModel {
    public static final int TAB_INDEX = 2;
    private final StateManager stateManager;
    private final List<BibEntry> selectedEntries;

    public ClearContentViewModel(List<BibEntry> selectedEntries, StateManager stateManager) {
        this.stateManager = stateManager;
        this.selectedEntries = new ArrayList<>(selectedEntries);
    }

    public Set<Field> getAllFields() {
        return FieldFactory.getAllFieldsWithOutInternal();
    }

    public void clearField(Field field) {
        NamedCompoundEdit edits = new NamedCompoundEdit("CLEAR_SELECTED_FIELD");
        List<BibEntry> selected = stateManager.getSelectedEntries();
        int affectedEntriesCount = 0;
        for (BibEntry entry : selected) {
            Optional<String> oldFieldValue = entry.getField(field);
            if (oldFieldValue.isPresent()) {
                entry.clearField(field)
                     .ifPresent(change -> edits.addEdit(new UndoableFieldChange(change)));
                affectedEntriesCount++;
            }
        }

        if (edits.hasEdits()) {
            edits.end();
        }
        stateManager.setLastAutomaticFieldEditorEdit(
                new LastAutomaticFieldEditorEdit(
                        affectedEntriesCount,
                        TAB_INDEX,
                        edits)
        );
    }
}
