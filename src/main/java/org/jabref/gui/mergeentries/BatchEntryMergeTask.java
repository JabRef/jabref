package org.jabref.gui.mergeentries;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private final MergeContext context;
    private final NamedCompound compoundEdit;
    private final AtomicInteger processedEntries;
    private final AtomicInteger successfulUpdates;

    public BatchEntryMergeTask(MergeContext context) {
        this.context = context;
        this.compoundEdit = new NamedCompound(Localization.lang("Merge entries"));
        this.processedEntries = new AtomicInteger(0);
        this.successfulUpdates = new AtomicInteger(0);

        configureTask();
    }

    private void configureTask() {
        setTitle(Localization.lang("Fetching and merging entries"));
        withInitialMessage(Localization.lang("Starting merge operation..."));
        showToUser(true);
    }

    @Override
    public List<String> call() throws Exception {
        try {
            List<String> updatedEntries = processMergeEntries();
            finalizeOperation(updatedEntries);
            LOGGER.debug("Merge operation completed. Processed: {}, Successfully updated: {}",
                    processedEntries.get(), successfulUpdates.get());
            notifySuccess(successfulUpdates.get());
            return updatedEntries;
        } catch (Exception e) {
            LOGGER.error("Critical error during merge operation", e);
            notifyError(e);
            throw e;
        }
    }

    private List<String> processMergeEntries() {
        return context.entries().stream()
                      .takeWhile(_ -> !isCancelled())
                      .map(this::processSingleEntryWithProgress)
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .collect(Collectors.toList());
    }

    private Optional<String> processSingleEntryWithProgress(BibEntry entry) {
        updateProgress(processedEntries.incrementAndGet(), context.entries().size());
        updateMessage(Localization.lang("Processing entry %0 of %1",
                processedEntries.get(),
                context.entries().size()));
        return processSingleEntry(entry);
    }

    private Optional<String> processSingleEntry(BibEntry entry) {
        try {
            LOGGER.debug("Processing entry: {}", entry);
            return context.fetcher().fetchEntry(entry)
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
        String errorMessage = String.format("Error processing entry %s: %s",
                citationKey,
                e.getMessage());

        LOGGER.error(errorMessage, e);
        context.notificationService().notify(Localization.lang(errorMessage));
    }

    private void finalizeOperation(List<String> updatedEntries) {
        if (!updatedEntries.isEmpty()) {
            synchronized (compoundEdit) {
                compoundEdit.end();
                context.undoManager.addEdit(compoundEdit);
            }
        }
    }

    private void notifySuccess(int updateCount) {
        String message = updateCount == 0
                ? Localization.lang("No updates found.")
                : Localization.lang("Batch update successful. %0 entries updated.", updateCount);
        context.notificationService().notify(message);
    }

    private void notifyError(Exception e) {
        context.notificationService().notify(
                Localization.lang("Merge operation failed: %0", e.getMessage()));
    }

    /**
     * Record containing all the context needed for the merge operation.
     * Implements defensive copying to ensure immutability.
     */
    public record MergeContext(
            List<BibEntry> entries,
            MergingIdBasedFetcher fetcher,
            UndoManager undoManager,
            NotificationService notificationService
    ) {
        public MergeContext {
            entries = List.copyOf(entries); // Defensive copy
        }
    }
}
