package org.jabref.gui.texparser;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ParseTexAction extends SimpleCommand {

    private final StateManager stateManager;

    public ParseTexAction(StateManager stateManager) {
        this.stateManager = stateManager;
        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(NullPointerException::new);
        ParseTexDialogView dialog = new ParseTexDialogView(database);

        dialog.showAndWait();
    }
}
