package org.jabref.gui.maintable;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

public class OpenFolderAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    public OpenFolderAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.isFieldSetForSelectedEntry(StandardField.FILE, stateManager));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext ->
            stateManager.getSelectedEntries().forEach(entry -> {
                LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                        entry.getFiles().get(0),
                        entry,
                        databaseContext,
                        Globals.TASK_EXECUTOR,
                        dialogService,
                        preferencesService.getXMPPreferences(),
                        preferencesService.getFilePreferences(),
                        ExternalFileTypes.getInstance());
                linkedFileViewModel.openFolder();
            }));
    }
}
