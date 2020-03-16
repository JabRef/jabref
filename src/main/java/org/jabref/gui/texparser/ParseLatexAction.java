package org.jabref.gui.texparser;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;

public class ParseLatexAction extends SimpleCommand {

    private final StateManager stateManager;

    public ParseLatexAction(StateManager stateManager) {
        this.stateManager = stateManager;
        executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(NullPointerException::new);
        ParseLatexDialogView dialog = new ParseLatexDialogView(database);
        dialog.showAndWait();
    }
}
