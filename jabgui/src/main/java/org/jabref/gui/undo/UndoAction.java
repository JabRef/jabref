package org.jabref.gui.undo;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.ChangeOutcome;

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

    @Override
    public void execute() {
        if (stateManager.activeTabProperty().get().isEmpty()) {
            return;
        }

        LibraryTab libraryTab = stateManager.activeTabProperty().get().get();
        GuiUndoManager undoManager = stateManager.getUndoManager(libraryTab.getBibDatabaseContext());

        Optional<String> writing = undoManager.writeInProgress();
        if (writing.isPresent()) {
            // Checked before canUndo(), which a reservation also makes false: the stack is not
            // empty, the library is busy, and saying "nothing to undo" would be untrue.
            dialogService.notify(Localization.lang("Cannot undo while %0 is running", writing.get()));
            return;
        }

        if (!undoManager.canUndo()) {
            dialogService.notify(Localization.lang("Nothing to undo") + '.');
            return;
        }

        undoManager.undo().ifPresent(outcome -> dialogService.notify(message(outcome)));
        libraryTab.markChangedOrUnChanged();
    }

    /// A set applies best-effort, so an undo can take back less than it names. Saying only what
    /// was undone would then be a message the library does not match.
    private static String message(ChangeOutcome outcome) {
        if (outcome.result().isComplete()) {
            return Localization.lang("Undone: %0", outcome.description());
        }
        return Localization.lang("Undone: %0 (some changes could not be applied)", outcome.description());
    }
}
