package org.jabref.gui.undo;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoStep;

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
                () -> redo(undoManager));
    }

    private void redo(GuiUndoManager undoManager) {
        undoManager.redo().ifPresentOrElse(
                step -> dialogService.notify(message(step)),
                () -> dialogService.notify(Localization.lang("Nothing to redo") + '.'));
    }

    private static String message(UndoStep step) {
        return step.complete()
               ? Localization.lang("Redone: %0", step.name())
               : Localization.lang("Redone: %0 (some changes could not be applied)", step.name());
    }
}
