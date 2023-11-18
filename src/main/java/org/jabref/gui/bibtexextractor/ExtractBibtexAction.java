package org.jabref.gui.bibtexextractor;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

import com.airhacks.afterburner.injection.Injector;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.GrobidOptInDialogHelper;
import org.jabref.preferences.PreferencesService;

public class ExtractBibtexAction extends SimpleCommand {

    PreferencesService preferencesService;
    DialogService dialogService;

    public ExtractBibtexAction(
        DialogService dialogService,
        PreferencesService preferencesService,
        StateManager stateManager
    ) {
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        GrobidOptInDialogHelper.showAndWaitIfUserIsUndecided(
            dialogService,
            preferencesService.getGrobidPreferences()
        );
        dialogService.showCustomDialogAndWait(new ExtractBibtexDialog());
    }
}
