package org.jabref.gui.importer.actions;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.WebFetchers;
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
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty() || stateManager.getSelectedEntries().isEmpty()) {
            return;
        }

        BibEntry selectedEntry = stateManager.getSelectedEntries().get(0);
        MultiMergeEntriesView mergeView = new MultiMergeEntriesView(preferences, taskExecutor);
        mergeView.addSource("Original", () -> selectedEntry);

        var fetchers = WebFetchers.getEntryBasedFetchers(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getFilePreferences(),
                stateManager.getActiveDatabase().get()
        );

        for (var fetcher : fetchers) {
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
