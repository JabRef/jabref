package org.jabref.gui.bibtexextractor;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.GrobidOptInDialogHelper;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ExtractBibtexAction extends SimpleCommand {

    DialogService dialogService;
    PreferencesService preferencesService;

    public ExtractBibtexAction(DialogService dialogService, PreferencesService preferencesService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        // Probably better to inject in constructor
        GrobidOptInDialogHelper.showAndWaitIfUserIsUndecided(
                dialogService,
                preferencesService.importSettingsPreferencesSupplier(),
                preferencesService.importSettingsPreferencesRetainer());
        dialogService.showCustomDialogAndWait(new ExtractBibtexDialog());
    }
}
