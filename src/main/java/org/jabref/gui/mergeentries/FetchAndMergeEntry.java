
package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchAndMergeEntry {


    public static final List<Field> SUPPORTED_IDENTIFIER_FIELDS = Arrays.asList(StandardField.DOI, StandardField.EPRINT, StandardField.ISBN);

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchAndMergeEntry.class);

    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final GuiPreferences preferences;

    public FetchAndMergeEntry(BibDatabaseContext bibDatabaseContext,
                              TaskExecutor taskExecutor,
                              GuiPreferences preferences,
                              DialogService dialogService,
                              UndoManager undoManager) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
    }

    public void fetchAndMerge(BibEntry entry) {
        fetchAndMerge(entry, SUPPORTED_IDENTIFIER_FIELDS);
    }

    public void fetchAndMerge(BibEntry entry, Field field) {
        fetchAndMerge(entry, List.of(field));
    }

    public void fetchAndMerge(BibEntry entry, List<Field> fields) {
        fields.forEach(field -> fetchAndMergeEntry(entry, field));
    }

    private void fetchAndMergeEntry(BibEntry entry, Field field) {
        entry.getField(field)
             .flatMap(fieldContent -> WebFetchers.getIdBasedFetcherForField(field, preferences.getImportFormatPreferences()))
             .ifPresent(fetcher -> executeFetchTask(fetcher, field, entry));
    }

    private void executeFetchTask(IdBasedFetcher fetcher, Field field, BibEntry entry) {
        entry.getField(field).ifPresent(fieldContent ->
                BackgroundTask.wrap(() -> fetcher.performSearchById(fieldContent))
                              .onSuccess(fetchedEntry -> processFetchedEntry(fetchedEntry, entry, fetcher))
                              .onFailure(exception -> handleFetchException(exception, fetcher))
                              .executeWith(taskExecutor)
        );
    }

    private void processFetchedEntry(Optional<BibEntry> fetchedEntry, BibEntry originalEntry, IdBasedFetcher fetcher) {
        ImportCleanup cleanup = ImportCleanup.targeting(bibDatabaseContext.getMode(), preferences.getFieldPreferences());
        fetchedEntry.ifPresentOrElse(
                entry -> {
                    cleanup.doPostCleanup(entry);
                    showMergeDialog(originalEntry, entry, fetcher);
                },
                () -> notifyNoInfo(originalEntry)
        );
    }

    private void notifyNoInfo(BibEntry entry) {
        dialogService.notify(Localization.lang("Cannot get info based on given %0: %1",
                entry.getType().getDisplayName(),
                entry.getCitationKey().orElse("")));
    }

    private void handleFetchException(Exception exception, WebFetcher fetcher) {
        LOGGER.error("Error while fetching bibliographic information", exception);
        dialogService.showErrorDialogAndWait(
                Localization.lang("Error while fetching from %0", fetcher.getName()),
                exception
        );
    }

    private void showMergeDialog(BibEntry originalEntry, BibEntry fetchedEntry, WebFetcher fetcher) {
        MergeEntriesDialog dialog = createMergeDialog(originalEntry, fetchedEntry, fetcher);
        Optional<BibEntry> mergedEntry = dialogService.showCustomDialogAndWait(dialog)
                                                      .map(EntriesMergeResult::mergedEntry);

        mergedEntry.ifPresentOrElse(
                entry -> processMergedEntry(originalEntry, entry, fetcher),
                () -> notifyCanceledMerge(originalEntry)
        );
    }

    private MergeEntriesDialog createMergeDialog(BibEntry originalEntry, BibEntry fetchedEntry, WebFetcher fetcher) {
        MergeEntriesDialog dialog = new MergeEntriesDialog(originalEntry, fetchedEntry, preferences);
        dialog.setTitle(Localization.lang("Merge entry with %0 information", fetcher.getName()));
        dialog.setLeftHeaderText(Localization.lang("Original entry"));
        dialog.setRightHeaderText(Localization.lang("Entry from %0", fetcher.getName()));
        return dialog;
    }

    private void processMergedEntry(BibEntry originalEntry, BibEntry mergedEntry, WebFetcher fetcher) {
        NamedCompound ce = new NamedCompound(Localization.lang("Merge entry with %0 information", fetcher.getName()));
        MergeEntriesHelper.mergeEntries(originalEntry, mergedEntry, ce);

        if (ce.hasEdits()) {
            ce.end();
            undoManager.addEdit(ce);
        }

        dialogService.notify(Localization.lang("Updated entry with info from %0", fetcher.getName()));
    }

    private void notifyCanceledMerge(BibEntry entry) {
        String citationKey = entry.getCitationKey().orElse(entry.getAuthorTitleYear(40));
        dialogService.notify(Localization.lang("Canceled merging entries") + " [" + citationKey + "]");
    }

    public void fetchAndMerge(BibEntry entry, EntryBasedFetcher fetcher) {
        BackgroundTask.wrap(() -> fetcher.performSearch(entry).stream().findFirst())
                      .onSuccess(fetchedEntry -> processFetchedEntryForEntryBasedFetcher(fetchedEntry, entry, fetcher))
                      .onFailure(exception -> handleFetchException(exception, fetcher))
                      .executeWith(taskExecutor);
    }

    private void processFetchedEntryForEntryBasedFetcher(Optional<BibEntry> fetchedEntry, BibEntry originalEntry, EntryBasedFetcher fetcher) {
        fetchedEntry
                .map(this::cleanupFetchedEntry)
                .ifPresentOrElse(
                        fe -> showMergeDialog(originalEntry, fe, fetcher),
                        () -> dialogService.notify(Localization.lang("Could not find any bibliographic information."))
                );
    }

    private BibEntry cleanupFetchedEntry(BibEntry fetchedEntry) {
        ImportCleanup cleanup = ImportCleanup.targeting(bibDatabaseContext.getMode(), preferences.getFieldPreferences());
        cleanup.doPostCleanup(fetchedEntry);
        return fetchedEntry;
    }
}
