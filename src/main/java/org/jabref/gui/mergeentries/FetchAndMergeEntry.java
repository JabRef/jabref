
package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherServerException;
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
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for fetching and merging bibliographic information
 */
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
        fields.forEach(field -> fetchAndMergeEntryWithDialog(entry, field));
    }

    private void fetchAndMergeEntryWithDialog(BibEntry entry, Field field) {
        entry.getField(field)
             .flatMap(fieldContent -> WebFetchers.getIdBasedFetcherForField(field, preferences.getImportFormatPreferences()))
             .ifPresent(fetcher -> executeFetchTaskWithDialog(fetcher, field, entry));
    }

    private void executeFetchTaskWithDialog(IdBasedFetcher fetcher, Field field, BibEntry entry) {
        entry.getField(field)
             .ifPresent(fieldContent ->
                     BackgroundTask.wrap(() -> fetcher.performSearchById(fieldContent))
                                   .onSuccess(fetchedEntry -> processFetchedEntryWithDialog(fetchedEntry, field, entry, fetcher))
                                   .onFailure(exception -> handleFetchException(exception, fetcher))
                                   .executeWith(taskExecutor)
             );
    }

    private void processFetchedEntryWithDialog(Optional<BibEntry> fetchedEntry, Field field, BibEntry originalEntry, IdBasedFetcher fetcher) {
        ImportCleanup cleanup = ImportCleanup.targeting(bibDatabaseContext.getMode(), preferences.getFieldPreferences());
        fetchedEntry.ifPresentOrElse(
                entry -> {
                    cleanup.doPostCleanup(entry);
                    showMergeDialog(originalEntry, entry, fetcher);
                },
                () -> dialogService.notify(Localization.lang("Cannot get info based on given %0: %1", field.getDisplayName(), originalEntry.getField(field).orElse("")))
        );
    }

    public void fetchAndMergeBatch(List<BibEntry> entries) {
        entries.forEach(entry -> SUPPORTED_IDENTIFIER_FIELDS.forEach(field -> fetchAndMergeEntry(entry, field)));
    }

    private void fetchAndMergeEntry(BibEntry entry, Field field) {
        entry.getField(field)
             .flatMap(fieldContent -> WebFetchers.getIdBasedFetcherForField(field, preferences.getImportFormatPreferences()))
             .ifPresent(fetcher -> executeFetchTask(fetcher, field, entry));
    }

    private void executeFetchTask(IdBasedFetcher fetcher, Field field, BibEntry entry) {
        entry.getField(field)
             .ifPresent(fieldContent -> BackgroundTask.wrap(() -> fetcher.performSearchById(fieldContent))
                                                      .onSuccess(fetchedEntry -> processFetchedEntry(fetchedEntry, field, entry))
                                                      .onFailure(exception -> handleFetchException(exception, fetcher))
                                                      .executeWith(taskExecutor));
    }

    private void processFetchedEntry(Optional<BibEntry> fetchedEntry, Field field, BibEntry originalEntry) {
        ImportCleanup cleanup = ImportCleanup.targeting(bibDatabaseContext.getMode(), preferences.getFieldPreferences());
        fetchedEntry.ifPresentOrElse(
                entry -> {
                    cleanup.doPostCleanup(entry);
                    mergeWithoutDialog(originalEntry, entry);
                },
                // Notify if no entry was fetched
                () -> dialogService.notify(Localization.lang("Cannot get info based on given %0: %1", field.getDisplayName(), originalEntry.getField(field).orElse("")))
        );
    }

    private void handleFetchException(Exception exception, IdBasedFetcher fetcher) {
        LOGGER.error("Error while fetching bibliographic information", exception);
        String fetcherName = fetcher.getName();
        // Handle different types of exceptions with specific error messages
        if (exception instanceof FetcherClientException) {
            dialogService.showInformationDialogAndWait(Localization.lang("Fetching information using %0", fetcherName), Localization.lang("No data was found for the identifier"));
        } else if (exception instanceof FetcherServerException) {
            dialogService.showInformationDialogAndWait(Localization.lang("Fetching information using %0", fetcherName), Localization.lang("Server not available"));
        } else {
            dialogService.showInformationDialogAndWait(Localization.lang("Fetching information using %0", fetcherName), Localization.lang("Error occurred %0", exception.getMessage()));
        }
    }

    private void mergeWithoutDialog(BibEntry originalEntry, BibEntry fetchedEntry) {
        NamedCompound ce = new NamedCompound(Localization.lang("Merge entry without user interaction"));

        updateEntryTypeIfDifferent(originalEntry, fetchedEntry, ce);
        updateFieldsWithNewInfo(originalEntry, fetchedEntry, ce);
        removeObsoleteFields(originalEntry, fetchedEntry, ce);

        finalizeMerge(ce, originalEntry);
    }

    private void updateFieldsWithNewInfo(BibEntry originalEntry, BibEntry fetchedEntry, NamedCompound ce) {
        Set<Field> jointFields = getJointFields(originalEntry, fetchedEntry);
        for (Field field : jointFields) {
            updateFieldIfNecessary(originalEntry, fetchedEntry, field, ce);
        }
    }

    private void updateFieldIfNecessary(BibEntry originalEntry, BibEntry fetchedEntry, Field field, NamedCompound ce) {
        fetchedEntry.getField(field).ifPresent(fetchedValue -> {
            Optional<String> originalValue = originalEntry.getField(field);
            if (originalValue.isEmpty() || fetchedValue.length() > originalValue.get().length()) {
                originalEntry.setField(field, fetchedValue);
                ce.addEdit(new UndoableFieldChange(originalEntry, field, originalValue.orElse(null), fetchedValue));
            }
        });
    }

    private void removeObsoleteFields(BibEntry originalEntry, BibEntry fetchedEntry, NamedCompound ce) {
        Set<Field> jointFields = getJointFields(originalEntry, fetchedEntry);
        Set<Field> originalFields = getFields(originalEntry);

        for (Field field : originalFields) {
            if (!jointFields.contains(field) && !FieldFactory.isInternalField(field)) {
                removeField(originalEntry, field, ce);
            }
        }
    }

    private void removeField(BibEntry entry, Field field, NamedCompound ce) {
        Optional<String> originalValue = entry.getField(field);
        entry.clearField(field);
        ce.addEdit(new UndoableFieldChange(entry, field, originalValue.orElse(null), null));
    }

    private void finalizeMerge(NamedCompound ce, BibEntry entry) {
        String citationKey = entry.getCitationKey().orElse(entry.getAuthorTitleYear(40));
        String message = ce.hasEdits()
                ? Localization.lang("Updated entry with fetched information [%0]", citationKey)
                : Localization.lang("No new information was added [%0]", citationKey);

        if (ce.hasEdits()) {
            ce.end();
            undoManager.addEdit(ce);
        }
        dialogService.notify(message);
    }

    private Set<Field> getFields(BibEntry entry) {
        // Get sorted set of fields for consistent ordering
        return entry.getFields().stream()
                    .sorted(Comparator.comparing(Field::getName))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Field> getJointFields(BibEntry entry1, BibEntry entry2) {
        Set<Field> fields = new LinkedHashSet<>();
        fields.addAll(getFields(entry1));
        fields.addAll(getFields(entry2));
        return fields;
    }

    private void updateEntryTypeIfDifferent(BibEntry originalEntry, BibEntry fetchedEntry, NamedCompound ce) {
        EntryType oldType = originalEntry.getType();
        EntryType newType = fetchedEntry.getType();

        if (!oldType.equals(newType)) {
            originalEntry.setType(newType);
            ce.addEdit(new UndoableChangeType(originalEntry, oldType, newType));
        }
    }

    private void showMergeDialog(BibEntry originalEntry, BibEntry fetchedEntry, WebFetcher fetcher) {
        MergeEntriesDialog dialog = createMergeDialog(originalEntry, fetchedEntry, fetcher);
        Optional<BibEntry> mergedEntry = showDialogAndGetResult(dialog);

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

    private Optional<BibEntry> showDialogAndGetResult(MergeEntriesDialog dialog) {
        return dialogService.showCustomDialogAndWait(dialog)
                            .map(EntriesMergeResult::mergedEntry);
    }

    private void processMergedEntry(BibEntry originalEntry, BibEntry mergedEntry, WebFetcher fetcher) {
        NamedCompound ce = new NamedCompound(Localization.lang("Merge entry with %0 information", fetcher.getName()));

        updateEntryTypeIfDifferent(originalEntry, mergedEntry, ce);
        updateFieldsWithNewInfo(originalEntry, mergedEntry, ce);
        removeObsoleteFields(originalEntry, mergedEntry, ce);

        finalizeMerge(ce, originalEntry);
    }

    private void notifyCanceledMerge(BibEntry entry) {
        String citationKey = entry.getCitationKey().orElse(entry.getAuthorTitleYear(40));
        dialogService.notify(Localization.lang("Canceled merging entries") + " [" + citationKey + "]");
    }

    public void fetchAndMerge(BibEntry entry, EntryBasedFetcher fetcher) {
        BackgroundTask.wrap(() -> fetcher.performSearch(entry).stream().findFirst())
                      .onSuccess(fetchedEntry -> fetchedEntry
                              .map(fe -> {
                                  ImportCleanup cleanup = ImportCleanup.targeting(bibDatabaseContext.getMode(), preferences.getFieldPreferences());
                                  cleanup.doPostCleanup(fe);
                                  return fe;
                              })
                              .ifPresentOrElse(
                                      fe -> showMergeDialog(entry, fe, fetcher),
                                      () -> dialogService.notify(Localization.lang("Could not find any bibliographic information."))
                              ))
                      .onFailure(exception -> {
                          LOGGER.error("Error while fetching entry with {} ", fetcher.getName(), exception);
                          dialogService.showErrorDialogAndWait(Localization.lang("Error while fetching from %0", fetcher.getName()), exception);
                      })
                      .executeWith(taskExecutor);
    }
}
