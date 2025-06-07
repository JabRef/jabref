package org.jabref.gui.mergeentries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.undo.UndoManager;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.importer.fetcher.MergingIdBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A background task that handles fetching and merging of bibliography entries.
 * This implementation provides improved concurrency handling, better Optional usage,
 * and more robust error handling.
 */
public class BatchEntryMergeTask extends BackgroundTask<List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchEntryMergeTask.class);

    private final NamedCompound compoundEdit;
    private final AtomicInteger processedEntries;
    private final AtomicInteger successfulUpdates;
    private final List<BibEntry> entries;
    private final MergingIdBasedFetcher fetcher;
    private final UndoManager undoManager;
    private final NotificationService notificationService;

    public BatchEntryMergeTask(List<BibEntry> entries,
                               MergingIdBasedFetcher fetcher,
                               UndoManager undoManager,
                               NotificationService notificationService) {
        this.entries = entries;
        this.fetcher = fetcher;
        this.undoManager = undoManager;
        this.notificationService = notificationService;

        this.compoundEdit = new NamedCompound(Localization.lang("Merge entries"));
        this.processedEntries = new AtomicInteger(0);
        this.successfulUpdates = new AtomicInteger(0);

        configureTask();
    }

    private void configureTask() {
        setTitle(Localization.lang("Fetching and merging entry(s)"));
        withInitialMessage(Localization.lang("Starting merge operation..."));
        showToUser(true);
    }

    @Override
    public List<String> call() {
            if (isCancelled()) {
                notifyCancellation();
                return List.of();
            }

            List<String> updatedEntries = processMergeEntries();

            if (isCancelled()) {
                notifyCancellation();
                finalizeOperation(updatedEntries);
                return updatedEntries;
            }

            finalizeOperation(updatedEntries);
            LOGGER.debug("Merge operation completed. Processed: {}, Successfully updated: {}",
                    processedEntries.get(), successfulUpdates.get());
            notifySuccess(successfulUpdates.get());
            return updatedEntries;
    }

    private void notifyCancellation() {
        LOGGER.debug("Merge operation was cancelled. Processed: {}, Successfully updated: {}",
                processedEntries.get(), successfulUpdates.get());
        notificationService.notify(
                Localization.lang("Merge operation cancelled after updating %0 entry(s)", successfulUpdates.get()));
    }

    private List<String> processMergeEntries() {
        List<String> updatedEntries = new ArrayList<>();

        for (BibEntry entry : entries) {
            Optional<String> result = processSingleEntryWithProgress(entry);
            result.ifPresent(updatedEntries::add);

            if (isCancelled()) {
                LOGGER.debug("Cancellation requested after processing entry {}", processedEntries.get());
                break;
            }
        }

        return updatedEntries;
    }

    private Optional<String> processSingleEntryWithProgress(BibEntry entry) {
        updateProgress(processedEntries.incrementAndGet(), entries.size());
        updateMessage(Localization.lang("Processing entry %0 of %1",
                processedEntries.get(),
                entries.size()));
        return processSingleEntry(entry);
    }

    private Optional<String> processSingleEntry(BibEntry entry) {
        try {
            LOGGER.debug("Processing entry: {}", entry);
            return fetcher.fetchEntry(entry)
                          .filter(MergingIdBasedFetcher.FetcherResult::hasChanges)
                          .flatMap(result -> {
                              boolean changesApplied = applyMerge(entry, result);
                              if (changesApplied) {
                                  successfulUpdates.incrementAndGet();
                                  return entry.getCitationKey();
                              }
                              return Optional.empty();
                          });
        } catch (Exception e) {
            handleEntryProcessingError(entry, e);
            return Optional.empty();
        }
    }

    private boolean applyMerge(BibEntry entry, MergingIdBasedFetcher.FetcherResult result) {
        synchronized (compoundEdit) {
            try {
                return MergeEntriesHelper.mergeEntries(result.mergedEntry(), entry, compoundEdit);
            } catch (Exception e) {
                LOGGER.error("Error during merge operation for entry: {}", entry, e);
                return false;
            }
        }
    }

    private void handleEntryProcessingError(BibEntry entry, Exception e) {
        String citationKey = entry.getCitationKey().orElse("unknown");
        String message = Localization.lang("Error processing entry", citationKey, e.getMessage());
        LOGGER.error(message, e);
        notificationService.notify(message);
    }

    private void finalizeOperation(List<String> updatedEntries) {
        if (!updatedEntries.isEmpty()) {
            synchronized (compoundEdit) {
                compoundEdit.end();
                undoManager.addEdit(compoundEdit);
            }
        }
    }

    private void notifySuccess(int updateCount) {
        String message = updateCount == 0
                ? Localization.lang("No updates found.")
                : Localization.lang("Batch update successful. %0 entry(s) updated.", updateCount);
        notificationService.notify(message);
    }
}
