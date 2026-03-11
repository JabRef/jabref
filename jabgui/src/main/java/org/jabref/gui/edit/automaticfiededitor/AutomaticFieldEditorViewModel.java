package org.jabref.gui.edit.automaticfiededitor;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.clearcontent.ClearContentTabView;
import org.jabref.gui.edit.automaticfiededitor.copyormovecontent.CopyOrMoveFieldContentTabView;
import org.jabref.gui.edit.automaticfiededitor.editfieldcontent.EditFieldContentTabView;
import org.jabref.gui.edit.automaticfiededitor.renamefield.RenameFieldTabView;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.model.database.BibDatabase;

public class AutomaticFieldEditorViewModel extends AbstractViewModel {
    public static final String NAMED_COMPOUND_EDITS = "EDIT_FIELDS";
    private final ObservableList<AutomaticFieldEditorTab> fieldEditorTabs = FXCollections.observableArrayList();
    private final NamedCompoundEdit dialogEdits = new NamedCompoundEdit(NAMED_COMPOUND_EDITS);

    private final UndoManager undoManager;

    public AutomaticFieldEditorViewModel(BibDatabase database,
                                         UndoManager undoManager,
                                         DialogService dialogService,
                                         StateManager stateManager) {
        this.undoManager = undoManager;
        fieldEditorTabs.addAll(
                new EditFieldContentTabView(database, dialogEdits, dialogService, stateManager),
                new CopyOrMoveFieldContentTabView(database, dialogEdits, dialogService, stateManager),
                new ClearContentTabView(database, dialogEdits, dialogService, stateManager),
                new RenameFieldTabView(database, dialogEdits, dialogService, stateManager)
        );
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
