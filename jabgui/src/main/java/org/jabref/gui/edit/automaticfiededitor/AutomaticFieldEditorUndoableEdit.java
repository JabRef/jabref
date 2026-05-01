package org.jabref.gui.edit.automaticfiededitor;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.jabref.gui.undo.NamedCompoundEdit;

public class AutomaticFieldEditorUndoableEdit extends NamedCompoundEdit {
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

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
    }
}
