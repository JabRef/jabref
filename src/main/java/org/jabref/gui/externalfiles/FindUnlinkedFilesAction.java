package org.jabref.gui.externalfiles;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class FindUnlinkedFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final UndoManager undoManager;
    private final StateManager stateManager;

    public FindUnlinkedFilesAction(DialogService dialogService, PreferencesService preferencesService, UndoManager undoManager, StateManager stateManager) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.undoManager = undoManager;
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        dialogService.showCustomDialogAndWait(new FindUnlinkedFilesDialog(database, dialogService, preferencesService, undoManager));
    }
}
