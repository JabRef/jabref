package org.jabref.gui.edit.automaticfiededitor;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.jabref.gui.undo.NamedCompoundEdit;

public class LastAutomaticFieldEditorEdit extends AbstractUndoableEdit {
    private final Integer affectedEntries;
    private final NamedCompoundEdit edit;

    private final Integer tabIndex;

    public LastAutomaticFieldEditorEdit(Integer affectedEntries, Integer tabIndex, NamedCompoundEdit edit) {
        this.affectedEntries = affectedEntries;
        this.edit = edit;
        this.tabIndex = tabIndex;
    }

    public Integer getAffectedEntries() {
        return affectedEntries;
    }

    public NamedCompoundEdit getEdit() {
        return edit;
    }

    public Integer getTabIndex() {
        return tabIndex;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        edit.undo();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        edit.redo();
    }
}
