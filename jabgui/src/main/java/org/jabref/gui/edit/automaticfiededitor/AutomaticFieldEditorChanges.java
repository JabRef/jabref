package org.jabref.gui.edit.automaticfiededitor;

import org.jabref.gui.undo.NamedCompoundEdit;

public class AutomaticFieldEditorChanges extends NamedCompoundEdit {
    int affectedEntries;

    public AutomaticFieldEditorChanges(String name) {
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
