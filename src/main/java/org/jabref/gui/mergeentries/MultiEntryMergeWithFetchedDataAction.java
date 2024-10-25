
package org.jabref.gui.mergeentries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.application.Platform;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.importer.fetcher.MergingIdBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the merging of multiple bibliography entries with fetched data.
 * This action performs background fetching and merging of entries while
 * providing progress updates to the user.
 */
public class MultiEntryMergeWithFetchedDataAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiEntryMergeWithFetchedDataAction.class);

    private final Supplier<LibraryTab> tabSupplier;
    private final GuiPreferences preferences;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;

    public MultiEntryMergeWithFetchedDataAction(Supplier<LibraryTab> tabSupplier,
                                                GuiPreferences preferences,
                                                NotificationService notificationService,
                                                StateManager stateManager,
                                                TaskExecutor taskExecutor) {
        this.tabSupplier = tabSupplier;
        this.preferences = preferences;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;
        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = tabSupplier.get();
        if (libraryTab == null) {
            LOGGER.error("Action 'Multi Entry Merge' must be disabled when no database is open.");
            return;
        }

        List<BibEntry> entries = libraryTab.getDatabase().getEntries();
        if (entries.isEmpty()) {
            notificationService.notify(Localization.lang("No entries exist."));
            return;
        }

        MergeContext context = new MergeContext(libraryTab, entries, createMergingFetcher());
        BackgroundTask<List<String>> backgroundTask = createBackgroundTask(context);
        taskExecutor.execute(backgroundTask);
    }

    private MergingIdBasedFetcher createMergingFetcher() {
        return new MergingIdBasedFetcher(preferences.getImportFormatPreferences());
    }

    private BackgroundTask<List<String>> createBackgroundTask(MergeContext context) {
        BackgroundTask<List<String>> task = new BackgroundTask<>() {
            @Override
            public List<String> call() {
                return processMergeEntries(context, this);
            }
        };
        configureBackgroundTask(task);
        return task;
    }

    private List<String> processMergeEntries(MergeContext context, BackgroundTask<?> task) {
        if (task.isCancelled()) {
            return List.of();
        }

        int totalEntries = context.entries.size();
        for (int i = 0; i < totalEntries && !task.isCancelled(); i++) {
            processEntry(context, context.entries.get(i), i, totalEntries, task);
        }

        finalizeCompoundEdit(context);
        return context.updatedEntries;
    }

    private void processEntry(MergeContext context, BibEntry entry, int currentIndex,
                              int totalEntries, BackgroundTask<?> task) {
        updateProgress(currentIndex, totalEntries, task);
        logEntryDetails(entry);

        Optional<MergingIdBasedFetcher.FetcherResult> fetchResult = context.fetcher.fetchEntry(entry);
        fetchResult.ifPresent(result -> {
            if (result.hasChanges()) {
                Platform.runLater(() -> {
                    // UI operations must be done on the FX thread
                    MergeEntriesHelper.mergeEntries(entry, result.mergedEntry(), context.compoundEdit);
                    entry.getCitationKey().ifPresent(context.updatedEntries::add);
                });
            }
        });
    }

    private void finalizeCompoundEdit(MergeContext context) {
        if (!context.updatedEntries.isEmpty()) {
            Platform.runLater(() -> {
                context.compoundEdit.end();
                context.libraryTab.getUndoManager().addEdit(context.compoundEdit);
            });
        }
    }

    private void logEntryDetails(BibEntry entry) {
        Map<String, String> details = new HashMap<>();
        entry.getCitationKey().ifPresent(key -> details.put("key", key));
        entry.getField(StandardField.ISBN).ifPresent(isbn -> details.put("isbn", isbn));
        entry.getField(StandardField.DOI).ifPresent(doi -> details.put("doi", doi));
        details.put("type", entry.getType().getName());

        LOGGER.info("Processing BibEntry: {}", details);
    }

    private void updateProgress(int currentIndex, int totalEntries, BackgroundTask<?> task) {
        Platform.runLater(() -> {
            task.updateMessage(Localization.lang("Fetching entry %0 of %1", currentIndex + 1, totalEntries));
            task.updateProgress(currentIndex, totalEntries);
        });
    }

    private void configureBackgroundTask(BackgroundTask<List<String>> task) {
        task.setTitle(Localization.lang("Fetching and merging entries"));
        task.showToUser(true);
        configureSuccessHandler(task);
        configureFailureHandler(task);
    }

    private void configureSuccessHandler(BackgroundTask<List<String>> task) {
        task.onSuccess(updatedEntries -> Platform.runLater(() -> {
            if (updatedEntries.isEmpty()) {
                LOGGER.info("Batch update completed. No entries were updated.");
                notificationService.notify(Localization.lang("No updates found."));
            } else {
                String message = Localization.lang("Batch update successful. %0 entries updated: %1.",
                        updatedEntries.size(), String.join(", ", updatedEntries));
                notificationService.notify(message);
            }
        }));
    }

    private void configureFailureHandler(BackgroundTask<List<String>> task) {
        task.onFailure(ex -> {
            String errorType = ex.getClass().getSimpleName();
            LOGGER.error("{}: {}", errorType, ex.getMessage(), ex);
            Platform.runLater(() ->
                    notificationService.notify(Localization.lang("Error while fetching and merging entries: %0", ex.getMessage()))
            );
        });
    }

    private static class MergeContext {
        final LibraryTab libraryTab;
        final List<BibEntry> entries;
        final MergingIdBasedFetcher fetcher;
        final NamedCompound compoundEdit;
        final List<String> updatedEntries;

        MergeContext(LibraryTab libraryTab, List<BibEntry> entries, MergingIdBasedFetcher fetcher) {
            this.libraryTab = libraryTab;
            this.entries = entries;
            this.fetcher = fetcher;
            this.compoundEdit = new NamedCompound(Localization.lang("Merge entries"));
            this.updatedEntries = Collections.synchronizedList(new ArrayList<>());
        }
    }
}
