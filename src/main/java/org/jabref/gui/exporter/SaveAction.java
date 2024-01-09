package org.jabref.gui.exporter;

import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
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
    private final Supplier<LibraryTab> tabSupplier;

    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    public SaveAction(SaveMethod saveMethod,
                      Supplier<LibraryTab> tabSupplier,
                      DialogService dialogService,
                      PreferencesService preferencesService,
                      StateManager stateManager) {
        this.saveMethod = saveMethod;
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
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
                tabSupplier.get(),
                dialogService,
                preferencesService,
                Globals.entryTypesManager);

        switch (saveMethod) {
            case SAVE -> saveDatabaseAction.save();
            case SAVE_AS -> saveDatabaseAction.saveAs();
            case SAVE_SELECTED -> saveDatabaseAction.saveSelectedAsPlain();
        }
    }
}
