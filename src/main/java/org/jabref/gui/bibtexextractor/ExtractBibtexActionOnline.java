package org.jabref.gui.bibtexextractor;

import javafx.beans.binding.Bindings;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.GrobidOptInDialogHelper;
import org.jabref.logic.preferences.CliPreferences;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ExtractBibtexActionOnline extends SimpleCommand {

    private final CliPreferences preferences;
    private final DialogService dialogService;

    public ExtractBibtexActionOnline(DialogService dialogService, CliPreferences preferences, StateManager stateManager, boolean requiresGrobid) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        if (requiresGrobid) {
            this.executable.bind(
                    Bindings.and(
                            preferences.getGrobidPreferences().grobidEnabledProperty(),
                            needsDatabase(stateManager)
                    ));
        } else {
            this.executable.bind(needsDatabase(stateManager));
        }
    }

    @Override
    public void execute() {
        boolean useGrobid = GrobidOptInDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferences.getGrobidPreferences());
        dialogService.showCustomDialogAndWait(new ExtractBibtexDialog(useGrobid));
    }
}
