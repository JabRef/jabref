package org.jabref.gui.texparser;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ParseTexAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final StateManager stateManager;

    public ParseTexAction(JabRefFrame jabRefFrame, StateManager stateManager) {
        this.jabRefFrame = jabRefFrame;
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase()
                                                  .orElseThrow(() -> new NullPointerException("No active database"));
        ParseTexDialog dialog = new ParseTexDialog(jabRefFrame.getDialogService(), database);

        dialog.showAndWait();
    }
}
