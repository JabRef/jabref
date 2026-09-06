package org.jabref.gui.undo;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.ChangeOutcome;

import org.jspecify.annotations.NullMarked;

import static org.jabref.gui.actions.ActionHelper.needsRedo;

/// Re-applies the last change undone in the library the user is looking at.
///
/// Reads the library when it runs, for the same reason as [UndoAction].
@NullMarked
public class RedoAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;

    public RedoAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(needsRedo(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.activeTabProperty().get().isEmpty()) {
            return;
        }

        LibraryTab libraryTab = stateManager.activeTabProperty().get().get();
        GuiUndoManager undoManager = stateManager.getUndoManager(libraryTab.getBibDatabaseContext());

        if (undoManager.canRedo()) {
            undoManager.redo().ifPresent(outcome -> dialogService.notify(message(outcome)));
        } else {
            dialogService.notify(Localization.lang("Nothing to redo") + '.');
        }
        libraryTab.markChangedOrUnChanged();
    }

    /// See [UndoAction#message].
    private static String message(ChangeOutcome outcome) {
        if (outcome.result().isComplete()) {
            return Localization.lang("Redone: %0", outcome.description());
        }
        return Localization.lang("Redone: %0 (some changes could not be applied)", outcome.description());
    }
}
