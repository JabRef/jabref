package org.jabref.gui.exporter;

import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

/**
 * This class is just a simple wrapper for the soon to be refactored SaveDatabaseAction.
 */
public class SaveAction extends SimpleCommand {

    public enum SaveMethod {
        SAVE, SAVE_AS, SAVE_SELECTED
    }

    private final SaveMethod saveMethod;
    private final JabRefFrame frame;

    public SaveAction(SaveMethod saveMethod, JabRefFrame frame, StateManager stateManager) {
        this.saveMethod = saveMethod;
        this.frame = frame;

        if (saveMethod == SaveMethod.SAVE_SELECTED) {
            this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
        } else {
            this.executable.bind(ActionHelper.needsDatabase(stateManager));
        }
    }

    @Override
    public void execute() {
        SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction(
                                                                       frame.getCurrentBasePanel(),
                                                                       Globals.prefs,
                                                                       Globals.entryTypesManager);
        GlobalSaveManager.addSaveAction(saveDatabaseAction, saveMethod, frame.getCurrentBasePanel().getBibDatabaseContext());

    }
}
