package org.jabref.gui.undo;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoStep;

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

        // A command holding the library is asked about before the stacks: a suspension makes
        // canUndo() false as well, and "nothing to undo" would then be untrue.
        undoManager.suspendedBy().ifPresentOrElse(
                command -> dialogService.notify(Localization.lang("Cannot undo while %0 is running", command)),
                () -> undo(undoManager));
    }

    private void undo(GuiUndoManager undoManager) {
        undoManager.undo().ifPresentOrElse(
                step -> dialogService.notify(message(step)),
                () -> dialogService.notify(Localization.lang("Nothing to undo") + '.'));
    }

    private static String message(UndoStep step) {
        return step.complete()
               ? Localization.lang("Undone: %0", step.name())
               : Localization.lang("Undone: %0 (some changes could not be applied)", step.name());
    }
}
