package org.jabref.gui.undo;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

import static org.jabref.gui.actions.ActionHelper.needsRedo;

/// Re-applies the last change undone in the library the user is looking at.
///
/// Reads the library when it runs, for the same reason as [UndoAction].
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
        Optional<LibraryTab> activeTab = stateManager.activeTabProperty().get();
        if (activeTab.isEmpty()) {
            return;
        }
        LibraryTab libraryTab = activeTab.get();
        GuiUndoManager undoManager = stateManager.getGuiUndoManager(libraryTab.getBibDatabaseContext());

        if (undoManager.canRedo()) {
            undoManager.redo();
            dialogService.notify(Localization.lang("Redo"));
        } else {
            dialogService.notify(Localization.lang("Nothing to redo") + '.');
        }
        libraryTab.markChangedOrUnChanged();
    }
}
