package org.jabref.gui.edit.automaticfiededitor.clearcontent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.LastAutomaticFieldEditorEdit;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class ClearContentViewModel {

    private final StateManager stateManager;

    public ClearContentViewModel(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    public Set<Field> getAllFields() {
        return FieldFactory.getAllFieldsWithOutInternal();
    }

    public Set<Field> getSetFieldsOnly() {
        return stateManager.getSelectedEntries().stream()
                           .flatMap(entry -> entry.getFields().stream()
                           .filter(f -> entry.getField(f).isPresent() && !entry.getField(f).get().isBlank()))
                           .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void clearField(Field field) {
        NamedCompoundEdit edits = new NamedCompoundEdit("Clear field content");
        List<BibEntry> selected = stateManager.getSelectedEntries();

        for (BibEntry entry : selected) {
            entry.clearField(field).ifPresent(change ->
                edits.addEdit(new UndoableFieldChange(change))
            );
        }

        if (edits.hasEdits()) {
            edits.end();
            stateManager.setLastAutomaticFieldEditorEdit(
                    new LastAutomaticFieldEditorEdit(selected.size(), 1, edits)
            );
        }
    }
}
