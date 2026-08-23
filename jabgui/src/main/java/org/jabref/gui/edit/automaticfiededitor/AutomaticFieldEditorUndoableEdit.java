package org.jabref.gui.edit.automaticfiededitor;

import org.jabref.gui.undo.CompoundEdit;

public class AutomaticFieldEditorUndoableEdit extends CompoundEdit {
    int affectedEntries;

    public AutomaticFieldEditorUndoableEdit(String name) {
        super(name);
        affectedEntries = 0;
    }

    public int getAffectedEntries() {
        return affectedEntries;
    }

    public void setAffectedEntries(int affectedEntries) {
        this.affectedEntries = affectedEntries;
    }
}
