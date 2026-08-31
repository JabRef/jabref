package org.jabref.gui.edit.automaticfieldeditor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfieldeditor.clearcontent.ClearContentTabView;
import org.jabref.gui.edit.automaticfieldeditor.copyormovecontent.CopyOrMoveFieldContentTabView;
import org.jabref.gui.edit.automaticfieldeditor.editfieldcontent.EditFieldContentTabView;
import org.jabref.gui.edit.automaticfieldeditor.renamefield.RenameFieldTabView;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.undo.CompoundEdit;

public class AutomaticFieldEditorViewModel extends AbstractViewModel {
    private final ObservableList<AutomaticFieldEditorTab> fieldEditorTabs = FXCollections.observableArrayList();

    /// One step for the whole dialog: every tab records into this, and OK pushes it as a single
    /// undo entry. Named after the dialog, because that is what the user acted in.
    private final CompoundEdit dialogEdits = new CompoundEdit(Localization.lang("Automatic field editor"));

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
        undoManager.addEdit(dialogEdits.toChangeSet());
    }

    /// Reverts what the tabs already wrote to the library, without recording anything.
    ///
    /// The one deliberate write outside the journal, and it stays outside on purpose: nothing
    /// was ever pushed — [#saveChanges] is what pushes — so there is no undo step to reverse and
    /// none to add. Cancelling leaves the library where it was before the dialog opened, which
    /// is exactly the state the top of the undo stack already describes.
    public void cancelChanges() {
        dialogEdits.toChangeSet().inverted().apply();
    }
}
