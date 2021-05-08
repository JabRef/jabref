package org.jabref.gui.maintable;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

public class OpenMultipleExternalFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    private final int maxNumberOfFiles = 20;

    public OpenMultipleExternalFilesAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.isFilePresentForSelectedEntry(stateManager, preferencesService));
    }

    @Override
    public void execute()
    {
        stateManager.getActiveDatabase().ifPresent(databaseContext ->
        {
            final List<BibEntry> selectedEntries = stateManager.getSelectedEntries();

            if (selectedEntries.size() != maxNumberOfFiles) {
                dialogService.notify(Localization.lang("This operation cannot exceed the max number of files to be opened at the same time, max is " + maxNumberOfFiles));
                return;
            }

            for (BibEntry selectedEntry : selectedEntries) {
                for (LinkedFile file : selectedEntry.getFiles()) {
                    LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                            file,
                            selectedEntry,
                            databaseContext,
                            Globals.TASK_EXECUTOR,
                            dialogService,
                            preferencesService.getXmpPreferences(),
                            preferencesService.getFilePreferences(),
                            ExternalFileTypes.getInstance());
                    linkedFileViewModel.open();
                }
            }
        });
    }
}
