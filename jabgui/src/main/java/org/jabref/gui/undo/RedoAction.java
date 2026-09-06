package org.jabref.gui.undo;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

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
            undoManager.redo();
            dialogService.notify(Localization.lang("Redo"));
        } else {
            dialogService.notify(Localization.lang("Nothing to redo") + '.');
        }
        libraryTab.markChangedOrUnChanged();
    }
}
