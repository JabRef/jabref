package org.jabref.gui.importer.actions;

import java.util.Set;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;

public class UpdateWithWebInfoAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;

    public UpdateWithWebInfoAction(StateManager stateManager,
                                   DialogService dialogService,
                                   GuiPreferences preferences,
                                   TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;

        // Bind enablement to selection
        this.executable.bind(ActionHelper.needsEntriesSelected(1, stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty() || stateManager.getSelectedEntries().isEmpty()) {
            return;
        }

        BibEntry selectedEntry = stateManager.getSelectedEntries().getFirst();

        MultiMergeEntriesView mergeView = new MultiMergeEntriesView(preferences, taskExecutor);

        mergeView.addSource(Localization.lang("Original"), () -> selectedEntry);

        Set<EntryBasedFetcher> fetchers = WebFetchers.getEntryBasedFetchers(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getFilePreferences(),
                stateManager.getActiveDatabase().get()
        );

        for (EntryBasedFetcher fetcher : fetchers) {
            mergeView.addSource(fetcher.getName(), () -> {
                try {
                    return fetcher.performSearch(selectedEntry).stream().findFirst().orElse(null);
                } catch (FetcherException e) {
                    return null;
                }
            });
        }

        dialogService.showCustomDialogAndWait(mergeView);
    }
}
