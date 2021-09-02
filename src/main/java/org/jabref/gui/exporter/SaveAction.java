package org.jabref.gui.exporter;

import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.preferences.PreferencesService;

/**
 * This class is just a simple wrapper for the soon to be refactored SaveDatabaseAction.
 */
public class SaveAction extends SimpleCommand {

    public enum SaveMethod { SAVE, SAVE_AS, SAVE_SELECTED }

    private final SaveMethod saveMethod;
    private final JabRefFrame frame;
    private final PreferencesService preferencesService;

    public SaveAction(SaveMethod saveMethod, JabRefFrame frame, PreferencesService preferencesService, StateManager stateManager) {
        this.saveMethod = saveMethod;
        this.frame = frame;
        this.preferencesService = preferencesService;

        if (saveMethod == SaveMethod.SAVE_SELECTED) {
            this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
        } else {
            this.executable.bind(ActionHelper.needsDatabase(stateManager));
        }
    }

    @Override
    public void execute() {
        SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction(
                frame.getCurrentLibraryTab(),
                preferencesService,
                Globals.entryTypesManager);

        switch (saveMethod) {
            case SAVE -> saveDatabaseAction.save();
            case SAVE_AS -> saveDatabaseAction.saveAs();
            case SAVE_SELECTED -> saveDatabaseAction.saveSelectedAsPlain();
        }
    }
}
