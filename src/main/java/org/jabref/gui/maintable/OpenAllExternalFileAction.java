package org.jabref.gui.maintable;

import java.util.ArrayList;
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
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAllExternalFileAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAllExternalFileAction.class);

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    public OpenAllExternalFileAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.isFilePresentForSelectedEntry(stateManager, preferencesService)
                .and(ActionHelper.needsMultipleEntriesSelected(stateManager))
        );
    }

    @Override
    public void execute() {
        LOGGER.info("Execute open ext file");
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            final List<BibEntry> selectedEntries = stateManager.getSelectedEntries();

            if (selectedEntries.size() <= 1) {
                dialogService.notify(Localization.lang("This operation requires more than one item to be selected."));
                return;
            }

            List<LinkedFileViewModel> linkedFileViewModelList = new ArrayList<>();
            LinkedFileViewModel linkedFileViewModel;

            boolean asked = false;

            for (BibEntry entry:selectedEntries) {
                if (entry.getFiles().isEmpty()) {
                    if (!asked) {
                        boolean continu = dialogService.showConfirmationDialogAndWait(Localization.lang("Missing file"),
                                Localization.lang("Some entries you selected are not linked to any file. They will be skipped. Continue?"),
                                "Continue", "Cancel");
                        asked = true;
                        if (!continu) {
                            return;
                        }
                    }
                } else {
                    linkedFileViewModel = new LinkedFileViewModel(
                            entry.getFiles().get(0),
                            entry,
                            databaseContext,
                            Globals.TASK_EXECUTOR,
                            dialogService,
                            preferencesService.getXmpPreferences(),
                            preferencesService.getFilePreferences(),
                            ExternalFileTypes.getInstance());

                    linkedFileViewModelList.add(linkedFileViewModel);
                }
            }

            linkedFileViewModelList.forEach(LinkedFileViewModel::open);
        });
    }
}
