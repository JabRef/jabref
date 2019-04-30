package org.jabref.gui.metadata;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class BibtexStringEditorAction extends SimpleCommand {

    private final StateManager stateManager;

    public BibtexStringEditorAction(StateManager stateManager) {
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        new BibtexStringEditorDialogView(database.getDatabase()).showAndWait();
    }
}
