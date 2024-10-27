package org.jabref.gui.mergeentries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class BatchEntryMergeWithFetchedDataAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchEntryMergeWithFetchedDataAction.class);

    private final Supplier<LibraryTab> tabSupplier;
    private final GuiPreferences preferences;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;

    public BatchEntryMergeWithFetchedDataAction(Supplier<LibraryTab> tabSupplier,
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
            LOGGER.error("Cannot perform batch merge: no library is open.");
            return;
        }

        List<BibEntry> libraryEntries = libraryTab.getDatabase().getEntries();
        if (libraryEntries.isEmpty()) {
            notificationService.notify(Localization.lang("empty library"));
            return;
        }

        MergeContext context = new MergeContext(
                libraryTab,
                libraryEntries,
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
                handleFailure(ex, context.notificationService()));

        return task;
    }

    private static List<String> processMergeEntries(MergeContext context, BackgroundTask<?> task) {
        int totalEntries = context.entries().size();

        for (int i = 0; i < totalEntries && !task.isCancelled(); i++) {
            BibEntry libraryEntry = context.entries().get(i);
            updateProgress(i, totalEntries, task);
            fetchAndMergeEntry(context, libraryEntry);
        }

        finalizeCompoundEdit(context);
        return context.updatedEntries();
    }

    private static void fetchAndMergeEntry(MergeContext context, BibEntry libraryEntry) {
        LOGGER.debug("Processing library entry: {}", libraryEntry);

        context.fetcher().fetchEntry(libraryEntry)
               .filter(MergingIdBasedFetcher.FetcherResult::hasChanges)
               .ifPresent(result -> Platform.runLater(() -> {
                   MergeEntriesHelper.mergeEntries(result.mergedEntry(), libraryEntry, context.compoundEdit());
                   libraryEntry.getCitationKey().ifPresent(context.updatedEntries()::add);
               }));
    }

    private static void finalizeCompoundEdit(MergeContext context) {
        if (!context.updatedEntries().isEmpty()) {
            Platform.runLater(() -> {
                context.compoundEdit().end();
                context.libraryTab().getUndoManager().addEdit(context.compoundEdit());
            });
        }
    }

    private static void updateProgress(int currentIndex, int totalEntries, BackgroundTask<?> task) {
        Platform.runLater(() -> {
            task.updateMessage(Localization.lang("Processing entry %0 of %1", currentIndex + 1, totalEntries));
            task.updateProgress(currentIndex, totalEntries);
        });
    }

    private static void handleSuccess(List<String> updatedEntries, MergeContext context) {
        Platform.runLater(() -> {
            String message = updatedEntries.isEmpty()
                    ? Localization.lang("No updates found.")
                    : Localization.lang("Batch update successful. %0 entries updated.",
                    updatedEntries.size());

            LOGGER.debug("Batch update completed. {}", message);
            context.notificationService().notify(message);
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
