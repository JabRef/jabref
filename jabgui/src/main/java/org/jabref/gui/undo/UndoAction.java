package org.jabref.gui.undo;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;

import static org.jabref.gui.actions.ActionHelper.needsUndo;

/// Undoes the last change made to the library the user is looking at.
///
/// The library, and so the journal, is read when the action runs rather than when it was built:
/// one instance serves every library the session opens.
@NullMarked
public class UndoAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;

    public UndoAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(needsUndo(stateManager));
    }

    private void undoIn(LibraryTab libraryTab) {
        GuiUndoManager undoManager = stateManager.getGuiUndoManager(libraryTab.getBibDatabaseContext());

        if (undoManager.canUndo()) {
            undoManager.undo();
            dialogService.notify(Localization.lang("Undo"));
        } else {
            dialogService.notify(Localization.lang("Nothing to undo") + '.');
        }
        libraryTab.markChangedOrUnChanged();
    }

    @Override
    public void execute() {
        stateManager.activeTabProperty().get().ifPresent(this::undoIn);
    }
}
