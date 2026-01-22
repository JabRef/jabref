package org.jabref.gui.cleanup;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntry;

public class CleanupSingleAction extends SimpleCommand {

    private final CliPreferences preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final BibEntry entry;
    private final UndoManager undoManager;

    public CleanupSingleAction(BibEntry entry,
                               CliPreferences preferences,
                               DialogService dialogService,
                               StateManager stateManager,
                               UndoManager undoManager) {
        this.entry = entry;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        CleanupDialog cleanupDialog = new CleanupDialog(
                entry,
                stateManager.getActiveDatabase().get(),
                preferences,
                dialogService,
                stateManager,
                undoManager
        );

        dialogService.showCustomDialogAndWait(cleanupDialog);
    }
}
