package org.jabref.gui.edit.automaticfiededitor.clearcontent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.LastAutomaticFieldEditorEdit;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.FieldChange;
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
        List<BibEntry> selected = stateManager.getSelectedEntries();
        Set<Field> setFields = new LinkedHashSet<>();

        for (BibEntry entry : selected) {
            for (Field f : entry.getFields()) {
                entry.getField(f).ifPresent(val -> {
                    if (!val.isBlank()) {
                        setFields.add(f);
                    }
                });
            }
        }
        return setFields;
    }

    public void clearField(Field field) {
        if (field == null) {
            return;
        }

        NamedCompound edits = new NamedCompound("Clear field content");
        int affected = 0;

        List<BibEntry> selected = stateManager.getSelectedEntries();
        for (BibEntry entry : selected) {
            // clearField returns Optional<FieldChange>
            entry.clearField(field).ifPresent((FieldChange change) -> {
                edits.addEdit(new UndoableFieldChange(change)); // Wrap into UndoableEdit
            });
        }

        if (edits.hasEdits()) {
            edits.end();
            stateManager.setLastAutomaticFieldEditorEdit(
                    new LastAutomaticFieldEditorEdit(selected.size(), 1, edits)
            );
        }
    }
}
