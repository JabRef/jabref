package org.jabref.gui.mergeentries;

import java.util.List;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.fetcher.MergingIdBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

/// Handles batch merging of bibliography entries with fetched data.
///
/// @see BatchEntryMergeTask
///
public class BatchEntryMergeWithFetchedDataAction extends SimpleCommand {

    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;

    public BatchEntryMergeWithFetchedDataAction(StateManager stateManager,
                                                GuiPreferences preferences,
                                                NotificationService notificationService,
                                                TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(this::mergeEntriesOf);
    }

    private void mergeEntriesOf(BibDatabaseContext databaseContext) {
        List<BibEntry> entries = databaseContext.getEntries();
        Character keywordSeparator = databaseContext.getKeywordSeparator(preferences.getBibEntryPreferences().getKeywordSeparator());

        if (entries.isEmpty()) {
            notificationService.notify(Localization.lang("No entries available for merging"));
            return;
        }

        MergingIdBasedFetcher fetcher = new MergingIdBasedFetcher(preferences.getImportFormatPreferences());
        BatchEntryMergeTask mergeTask = new BatchEntryMergeTask(
                entries,
                fetcher,
                stateManager.getUndoManager(databaseContext),
                notificationService,
                keywordSeparator);

        mergeTask.executeWith(taskExecutor);
    }
}
