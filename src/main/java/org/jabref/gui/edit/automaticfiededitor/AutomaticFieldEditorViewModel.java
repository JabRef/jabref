package org.jabref.gui.edit.automaticfiededitor;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.edit.automaticfiededitor.editfieldcontent.EditFieldContentTabView;
import org.jabref.gui.edit.automaticfiededitor.renamefield.RenameFieldTabView;
import org.jabref.gui.edit.automaticfiededitor.twofields.CopyOrMoveFieldContentTabView;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class AutomaticFieldEditorViewModel extends AbstractViewModel {
    public static final String NAMED_COMPOUND_EDITS = "EDIT_FIELDS";
    private final ObservableList<AutomaticFieldEditorTab> fieldEditorTabs = FXCollections.observableArrayList();
    private final NamedCompound dialogEdits = new NamedCompound(NAMED_COMPOUND_EDITS);

    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;

    public AutomaticFieldEditorViewModel(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext, UndoManager undoManager) {
        fieldEditorTabs.addAll(
                new EditFieldContentTabView(selectedEntries, databaseContext, dialogEdits),
                new CopyOrMoveFieldContentTabView(selectedEntries, databaseContext, dialogEdits),
                new RenameFieldTabView(selectedEntries, databaseContext, dialogEdits)
        );
        this.databaseContext = databaseContext;
        this.undoManager = undoManager;
    }

    public ObservableList<AutomaticFieldEditorTab> getFieldEditorTabs() {
        return fieldEditorTabs;
    }

    public void saveChanges() {
        dialogEdits.end();
        undoManager.addEdit(dialogEdits);
    }

    public void cancelChanges() {
        dialogEdits.end();
        dialogEdits.undo();
    }
}
