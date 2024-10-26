
package org.jabref.gui.mergeentries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        MergeContext context = new MergeContext(
                libraryTab,
                entries,
                new MergingIdBasedFetcher(preferences.getImportFormatPreferences()),
                notificationService
        );

        taskExecutor.execute(createBackgroundTask(context));
    }

    private static BackgroundTask<List<String>> createBackgroundTask(MergeContext context) {
        BackgroundTask<List<String>> task = new BackgroundTask<>() {
            @Override
             public List<String> call() {
                return processMergeEntries(context, this);
            }
        };

        task.setTitle(Localization.lang("Fetching and merging entries"));
        task.showToUser(true);

        task.onSuccess(updatedEntries ->
                handleSuccess(updatedEntries, context));

        task.onFailure(ex ->
                handleFailure(ex, context.notificationService));

        return task;
    }

    private static List<String> processMergeEntries(MergeContext context, BackgroundTask<?> task) {
        int totalEntries = context.entries().size();

        for (int i = 0; i < totalEntries && !task.isCancelled(); i++) {
            BibEntry entry = context.entries().get(i);
            updateProgress(i, totalEntries, entry, task);
            processEntry(context, entry);
        }

        finalizeCompoundEdit(context);
        return context.updatedEntries();
    }

    private static void processEntry(MergeContext context, BibEntry entry) {
        LOGGER.debug("Processing entry: {}", entry);

        Optional<MergingIdBasedFetcher.FetcherResult> fetchResult = context.fetcher().fetchEntry(entry);
        fetchResult.ifPresent(result -> {
            if (result.hasChanges()) {
                Platform.runLater(() -> {
                    MergeEntriesHelper.mergeEntries(entry, result.mergedEntry(), context.compoundEdit());
                    entry.getCitationKey().ifPresent(context.updatedEntries()::add);
                });
            }
        });
    }

    private static void finalizeCompoundEdit(MergeContext context) {
        if (!context.updatedEntries().isEmpty()) {
            Platform.runLater(() -> {
                context.compoundEdit().end();
                context.libraryTab().getUndoManager().addEdit(context.compoundEdit());
            });
        }
    }

    private static void updateProgress(int currentIndex, int totalEntries, BibEntry entry, BackgroundTask<?> task) {
        LOGGER.debug("Processing entry {}", entry);
        Platform.runLater(() -> {
            task.updateMessage(Localization.lang("Fetching entry %0 of %1", currentIndex + 1, totalEntries));
            task.updateProgress(currentIndex, totalEntries);
        });
    }

    private static void handleSuccess(List<String> updatedEntries, MergeContext context) {
        Platform.runLater(() -> {
            if (updatedEntries.isEmpty()) {
                LOGGER.debug("Batch update completed. No entries were updated.");
                context.notificationService().notify(Localization.lang("No updates found."));
            } else {
                LOGGER.debug("Updated entries: {}", String.join(", ", updatedEntries));

                String message = Localization.lang("Batch update successful. %0 entries updated.",
                        String.valueOf(updatedEntries.size()));
                context.notificationService().notify(message);
            }
        });
    }

    private static void handleFailure(Exception ex, NotificationService notificationService) {
        LOGGER.error("Error during fetch and merge", ex);
        Platform.runLater(() ->
                notificationService.notify(
                        Localization.lang("Error while fetching and merging entries: %0", ex.getMessage())
                )
        );
    }

    private record MergeContext(
            LibraryTab libraryTab,
            List<BibEntry> entries,
            MergingIdBasedFetcher fetcher,
            NamedCompound compoundEdit,
            List<String> updatedEntries,
            NotificationService notificationService
    ) {
        MergeContext(LibraryTab libraryTab, List<BibEntry> entries, MergingIdBasedFetcher fetcher, NotificationService notificationService) {
            this(
                    libraryTab,
                    entries,
                    fetcher,
                    new NamedCompound(Localization.lang("Merge entries")),
                    Collections.synchronizedList(new ArrayList<>()),
                    notificationService
            );
        }
    }
}
