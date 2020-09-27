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
import org.jabref.preferences.PreferencesService;

public class OpenExternalFileAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    public OpenExternalFileAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.isFilePresentForSelectedEntry(stateManager, preferencesService)
                                         .and(ActionHelper.needsEntriesSelected(1, stateManager)));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            final List<BibEntry> selectedEntries = stateManager.getSelectedEntries();

            if (selectedEntries.size() != 1) {
                dialogService.notify(Localization.lang("This operation requires exactly one item to be selected."));
                return;
            }

            final BibEntry entry = selectedEntries.get(0);

            LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                    entry.getFiles().get(0),
                    entry,
                    databaseContext,
                    Globals.TASK_EXECUTOR,
                    dialogService,
                    preferencesService.getXmpPreferences(),
                    preferencesService.getFilePreferences(),
                    ExternalFileTypes.getInstance());
            linkedFileViewModel.open();
        });
    }
}
