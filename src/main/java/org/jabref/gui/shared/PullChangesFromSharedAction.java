package org.jabref.gui.shared;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.shared.DatabaseSynchronizer;

public class PullChangesFromSharedAction extends SimpleCommand {

    private final StateManager stateManager;

    public PullChangesFromSharedAction(StateManager stateManager) {
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsDatabase(stateManager).and(ActionHelper.needsSharedDatabase(stateManager)));
    }

    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            DatabaseSynchronizer dbmsSynchronizer = databaseContext.getDBMSSynchronizer();
            dbmsSynchronizer.pullChanges();
        });
    }
}
