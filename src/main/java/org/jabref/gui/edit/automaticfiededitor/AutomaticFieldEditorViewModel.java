package org.jabref.gui.edit.automaticfiededitor;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.copyormovecontent.CopyOrMoveFieldContentTabView;
import org.jabref.gui.edit.automaticfiededitor.editfieldcontent.EditFieldContentTabView;
import org.jabref.gui.edit.automaticfiededitor.renamefield.RenameFieldTabView;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class AutomaticFieldEditorViewModel extends AbstractViewModel {
    public static final String NAMED_COMPOUND_EDITS = "EDIT_FIELDS";
    private final ObservableList<AutomaticFieldEditorTab> fieldEditorTabs = FXCollections.observableArrayList();
    private final NamedCompound dialogEdits = new NamedCompound(NAMED_COMPOUND_EDITS);

    private final UndoManager undoManager;

    public AutomaticFieldEditorViewModel(List<BibEntry> selectedEntries, BibDatabase database, UndoManager undoManager, StateManager stateManager) {
        this.undoManager = undoManager;
        fieldEditorTabs.addAll(
                new EditFieldContentTabView(database, stateManager),
                new CopyOrMoveFieldContentTabView(database, stateManager),
                new RenameFieldTabView(database, stateManager)
        );
    }

    public NamedCompound getDialogEdits() {
        return dialogEdits;
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
