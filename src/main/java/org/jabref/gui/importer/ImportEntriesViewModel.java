package org.jabref.gui.importer;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportEntriesViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportAction.class);

    private final StringProperty message;
    private final TaskExecutor taskExecutor;
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private ParserResult parserResult = null;
    private final ObservableList<BibEntry> entries;
    private final PreferencesService preferences;
    private final BibEntryTypesManager entryTypesManager;

    /**
     * @param databaseContext the database to import into
     * @param task            the task executed for parsing the selected files(s).
     */
    public ImportEntriesViewModel(BackgroundTask<ParserResult> task,
                                  TaskExecutor taskExecutor,
                                  BibDatabaseContext databaseContext,
                                  DialogService dialogService,
                                  UndoManager undoManager,
                                  PreferencesService preferences,
                                  StateManager stateManager,
                                  BibEntryTypesManager entryTypesManager,
                                  FileUpdateMonitor fileUpdateMonitor) {
        this.taskExecutor = taskExecutor;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.entryTypesManager = entryTypesManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entries = FXCollections.observableArrayList();
        this.message = new SimpleStringProperty();
        this.message.bind(task.messageProperty());

        task.onSuccess(parserResult -> {
            // store the complete parser result (to import groups, ... later on)
            this.parserResult = parserResult;
            // fill in the list for the user, where one can select the entries to import
            entries.addAll(parserResult.getDatabase().getEntries());
        }).onFailure(ex -> {
            LOGGER.error("Error importing", ex);
            dialogService.showErrorDialogAndWait(ex);
        }).executeWith(taskExecutor);
    }

    public String getMessage() {
        return message.get();
    }

    public StringProperty messageProperty() {
        return message;
    }

    public ObservableList<BibEntry> getEntries() {
        return entries;
    }

    public boolean hasDuplicate(BibEntry entry) {
        return findInternalDuplicate(entry).isPresent() ||
                new DuplicateCheck(entryTypesManager)
                .containsDuplicate(databaseContext.getDatabase(), entry, databaseContext.getMode()).isPresent();
    }

    /**
     * Called after the user selected the entries to import. Does the real import stuff.
     *
     * @param entriesToImport subset of the entries contained in parserResult
     */
    public void importEntries(List<BibEntry> entriesToImport, boolean shouldDownloadFiles) {
        // Check if we are supposed to warn about duplicates.
        // If so, then see if there are duplicates, and warn if yes.
        if (preferences.shouldWarnAboutDuplicatesForImport()) {
            BackgroundTask.wrap(() -> entriesToImport.stream()
                                                     .anyMatch(this::hasDuplicate)).onSuccess(duplicateFound -> {
                if (duplicateFound) {
                    boolean continueImport = dialogService.showConfirmationDialogWithOptOutAndWait(Localization.lang("Duplicates found"),
                            Localization.lang("There are possible duplicates that haven't been resolved. Continue?"),
                            Localization.lang("Continue with import"),
                            Localization.lang("Cancel import"),
                            Localization.lang("Do not ask again"),
                            optOut -> preferences.setShouldWarnAboutDuplicatesForImport(!optOut));

                    if (!continueImport) {
                        dialogService.notify(Localization.lang("Import canceled"));
                    } else {
                        buildImportHandlerThenImportEntries(entriesToImport);
                    }
                } else {
                    buildImportHandlerThenImportEntries(entriesToImport);
                }
            }).executeWith(taskExecutor);
        } else {
            buildImportHandlerThenImportEntries(entriesToImport);
        }

        // Remember the selection in the dialog
        preferences.getFilePreferences().setDownloadLinkedFiles(shouldDownloadFiles);

        if (shouldDownloadFiles) {
            for (BibEntry bibEntry : entriesToImport) {
                bibEntry.getFiles().stream().filter(LinkedFile::isOnlineLink).forEach(linkedFile ->
                        new LinkedFileViewModel(
                                linkedFile,
                                bibEntry,
                                databaseContext,
                                taskExecutor,
                                dialogService,
                                preferences,
                                ExternalFileTypes.getInstance()).download());
            }
        }

        new DatabaseMerger(preferences.getKeywordDelimiter()).mergeStrings(databaseContext.getDatabase(), parserResult.getDatabase());
        new DatabaseMerger(preferences.getKeywordDelimiter()).mergeMetaData(databaseContext.getMetaData(),
                parserResult.getMetaData(),
                parserResult.getPath().map(path -> path.getFileName().toString()).orElse("unknown"),
                parserResult.getDatabase().getEntries());

        JabRefGUI.getMainFrame().getCurrentLibraryTab().markBaseChanged();
    }

    private void buildImportHandlerThenImportEntries(List<BibEntry> entriesToImport) {
        ImportHandler importHandler = new ImportHandler(
                databaseContext,
                ExternalFileTypes.getInstance(),
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager);
        importHandler.importEntries(entriesToImport);
        dialogService.notify(Localization.lang("Number of entries successfully imported") + ": " + entriesToImport.size());
    }

    /**
     * Checks if there are duplicates to the given entry in the list of entries to be imported.
     *
     * @param entry The entry to search for duplicates of.
     * @return A possible duplicate, if any, or null if none were found.
     */
    private Optional<BibEntry> findInternalDuplicate(BibEntry entry) {
        for (BibEntry othEntry : entries) {
            if (othEntry.equals(entry)) {
                continue; // Don't compare the entry to itself
            }
            if (new DuplicateCheck(entryTypesManager).isDuplicate(entry, othEntry, databaseContext.getMode())) {
                return Optional.of(othEntry);
            }
        }
        return Optional.empty();
    }

    public void resolveDuplicate(BibEntry entry) {
        // First, try to find duplicate in the existing library
        Optional<BibEntry> other = new DuplicateCheck(entryTypesManager).containsDuplicate(databaseContext.getDatabase(), entry, databaseContext.getMode());
        if (other.isPresent()) {
            DuplicateResolverDialog dialog = new DuplicateResolverDialog(other.get(),
                    entry, DuplicateResolverDialog.DuplicateResolverType.INSPECTION, databaseContext, stateManager);

            DuplicateResolverDialog.DuplicateResolverResult result = dialogService.showCustomDialogAndWait(dialog)
                                                                                  .orElse(DuplicateResolverDialog.DuplicateResolverResult.BREAK);

            if (result == DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT) {
                // TODO: Remove old entry. Or... add it to a list of entries
                // to be deleted. We only delete
                // it after Ok is clicked.
                // entriesToDelete.add(other.get());
            } else if (result == DuplicateResolverDialog.DuplicateResolverResult.KEEP_RIGHT) {
                // Remove the entry from the import inspection dialog.
                entries.remove(entry);
            } else if (result == DuplicateResolverDialog.DuplicateResolverResult.KEEP_BOTH) {
                // Do nothing.
            } else if (result == DuplicateResolverDialog.DuplicateResolverResult.KEEP_MERGE) {
                // TODO: Remove old entry. Or... add it to a list of entries
                // to be deleted. We only delete
                // it after Ok is clicked.
                // entriesToDelete.add(other.get());

                // Replace entry by merged entry
                entries.add(dialog.getMergedEntry());
                entries.remove(entry);
            }
            return;
        }
        // Second, check if the duplicate is of another entry in the import:
        other = findInternalDuplicate(entry);
        if (other.isPresent()) {
            DuplicateResolverDialog diag = new DuplicateResolverDialog(entry,
                    other.get(), DuplicateResolverDialog.DuplicateResolverType.DUPLICATE_SEARCH, databaseContext, stateManager);

            DuplicateResolverDialog.DuplicateResolverResult answer = dialogService.showCustomDialogAndWait(diag)
                                                                                  .orElse(DuplicateResolverDialog.DuplicateResolverResult.BREAK);
            if (answer == DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT) {
                // Remove other entry
                entries.remove(other.get());
            } else if (answer == DuplicateResolverDialog.DuplicateResolverResult.KEEP_RIGHT) {
                // Remove entry
                entries.remove(entry);
            } else if (answer == DuplicateResolverDialog.DuplicateResolverResult.KEEP_BOTH) {
                // Do nothing
            } else if (answer == DuplicateResolverDialog.DuplicateResolverResult.KEEP_MERGE) {
                // Replace both entries by merged entry
                entries.add(diag.getMergedEntry());
                entries.remove(entry);
                entries.remove(other.get());
            }
        }
    }
}
