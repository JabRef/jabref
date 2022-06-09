package org.jabref.gui.edit.automaticfiededitor.editfieldvalue;

import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class EditFieldValueViewModel extends AbstractViewModel {
    private final BibDatabaseContext databaseContext;
    private final List<BibEntry> selectedEntries;
    private final UndoManager undoManager;

    public EditFieldValueViewModel(BibDatabaseContext databaseContext, List<BibEntry> selectedEntries, UndoManager undoManager) {
        this.databaseContext = databaseContext;
        this.selectedEntries = selectedEntries;
        this.undoManager = undoManager;
    }

    public void clearSelectedField() {
    }

    public void setFieldValue() {
    }

    public void appendToFieldValue() {
    }
}
