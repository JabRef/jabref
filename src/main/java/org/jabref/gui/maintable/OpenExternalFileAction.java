package org.jabref.gui.maintable;

import java.util.LinkedList;
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

public class OpenExternalFileAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    public OpenExternalFileAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.hasPresentFileForSelectedEntries(stateManager, preferencesService)
                .and(ActionHelper.needsEntriesSelected(stateManager))
        );
    }

    /**
     * Open all linked files of the selected entries.
     * <br>
     * If some selected entries have linked file and others does not, pop out a dialog to ask user whether to skip them and continue or cancel the whole action.
     */
    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            final List<BibEntry> selectedEntries = stateManager.getSelectedEntries();

            List<LinkedFileViewModel> linkedFileViewModelList = new LinkedList<>();
            LinkedFileViewModel linkedFileViewModel;

            boolean asked = false;

            for (BibEntry entry:selectedEntries) {
                if (entry.getFiles().isEmpty()) {
                    if (!asked) {
                        boolean continu = dialogService.showConfirmationDialogAndWait(Localization.lang("Missing file"),
                                Localization.lang("Some entries you selected are not linked to any file. They will be skipped. Continue?"),
                                Localization.lang("Continue"), Localization.lang("Cancel"));
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
