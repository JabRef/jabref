package org.jabref.gui.mergeentries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.fetcher.MergingIdBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A background task that handles fetching and merging of bibliography entries.
/// This implementation provides improved concurrency handling, better Optional usage,
/// and more robust error handling.
public class BatchEntryMergeTask extends BackgroundTask<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchEntryMergeTask.class);

    private final NamedCompoundEdit compoundEdit;
    private final List<BibEntry> entries;
    private final MergingIdBasedFetcher fetcher;
    private final UndoManager undoManager;
    private final NotificationService notificationService;

    private int processedEntries;
    private int successfulUpdates;

    public BatchEntryMergeTask(List<BibEntry> entries,
                               MergingIdBasedFetcher fetcher,
                               UndoManager undoManager,
                               NotificationService notificationService) {
        this.entries = entries;
        this.fetcher = fetcher;
        this.undoManager = undoManager;
        this.notificationService = notificationService;

        this.compoundEdit = new NamedCompoundEdit(Localization.lang("Merge entries"));
        this.processedEntries = 0;
        this.successfulUpdates = 0;

        setTitle(Localization.lang("Fetching and merging entry(s)"));
        withInitialMessage(Localization.lang("Starting merge operation..."));
        showToUser(true);
    }

    @Override
    public Void call() {
        if (isCancelled()) {
            notifyCancellation();
            return null;
        }

        List<String> updatedEntries = processMergeEntries();

        if (isCancelled()) {
            notifyCancellation();
            updateUndoManager(updatedEntries);
            return null;
        }

        updateUndoManager(updatedEntries);
        LOGGER.debug("Merge operation completed. Processed: {}, Successfully updated: {}",
                processedEntries, successfulUpdates);
        notifySuccess(successfulUpdates);
        return null;
    }

    private void notifyCancellation() {
        LOGGER.debug("Merge operation was cancelled. Processed: {}, Successfully updated: {}",
                processedEntries, successfulUpdates);
        notificationService.notify(
                Localization.lang("Merge operation cancelled after updating %0 entry(s)", successfulUpdates));
    }

    private List<String> processMergeEntries() {
        List<String> updatedEntries = new ArrayList<>(entries.size());

        for (BibEntry entry : entries) {
            processSingleEntryWithProgress(entry).ifPresent(updatedEntries::add);
            if (isCancelled()) {
                LOGGER.debug("Cancellation requested after processing entry {}", processedEntries);
                break;
            }
        }

        return updatedEntries;
    }

    private Optional<String> processSingleEntryWithProgress(BibEntry entry) {
        updateProgress(++processedEntries, entries.size());
        updateMessage(Localization.lang("Processing entry %0 of %1",
                processedEntries,
                entries.size()));
        return processSingleEntry(entry);
    }

    private Optional<String> processSingleEntry(BibEntry entry) {
        LOGGER.debug("Processing entry: {}", entry);
        return fetcher.fetchEntry(entry)
                      .filter(MergingIdBasedFetcher.FetcherResult::hasChanges)
                      .flatMap(result -> {
                          boolean changesApplied = MergeEntriesHelper.mergeEntries(result.mergedEntry(), entry, compoundEdit);
                          if (changesApplied) {
                              successfulUpdates++;
                              return entry.getCitationKey();
                          }
                          return Optional.empty();
                      });
    }

    private void updateUndoManager(List<String> updatedEntries) {
        if (!updatedEntries.isEmpty()) {
            compoundEdit.end();
            UiTaskExecutor.runInJavaFXThread(() -> undoManager.addEdit(compoundEdit));
        }
    }

    private void notifySuccess(int updateCount) {
        String message = updateCount == 0
                         ? Localization.lang("No updates found.")
                         : Localization.lang("Batch update successful. %0 entry(s) updated.", updateCount);
        notificationService.notify(message);
    }
}
