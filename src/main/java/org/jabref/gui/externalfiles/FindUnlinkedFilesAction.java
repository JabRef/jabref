package org.jabref.gui.externalfiles;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class FindUnlinkedFilesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final StateManager stateManager;

    public FindUnlinkedFilesAction(JabRefFrame jabRefFrame, StateManager stateManager) {
        this.jabRefFrame = jabRefFrame;
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        FindUnlinkedFilesDialog dlg = new FindUnlinkedFilesDialog(database, jabRefFrame.getDialogService(), jabRefFrame.getUndoManager());
        dlg.showAndWait();
    }
}
