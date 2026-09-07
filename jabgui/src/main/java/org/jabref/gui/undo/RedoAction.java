package org.jabref.gui.undo;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.StepOutcome;

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

        // See UndoAction: a suspension makes canRedo() false without the stack being empty.
        undoManager.suspendedBy().ifPresentOrElse(
                command -> dialogService.notify(Localization.lang("Cannot redo while %0 is running", command)),
                () -> redo(libraryTab, undoManager));
    }

    /// See [UndoAction#undo]: an empty outcome is the journal saying there was nothing to redo.
    private void redo(LibraryTab libraryTab, GuiUndoManager undoManager) {
        undoManager.redo().ifPresentOrElse(
                step -> {
                    dialogService.notify(message(step));
                    libraryTab.markChangedOrUnChanged();
                },
                () -> dialogService.notify(Localization.lang("Nothing to redo") + '.'));
    }

    /// See [UndoAction#message].
    private static String message(StepOutcome step) {
        return step.complete()
                ? Localization.lang("Redone: %0", step.name())
                : Localization.lang("Redone: %0 (some changes could not be applied)", step.name());
    }
}
