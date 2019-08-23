package org.jabref.gui.importer;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.Globals;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class ImportEntriesViewModel extends AbstractViewModel {

    private final BackgroundTask<List<BibEntry>> task;
    private final StringProperty message;
    private final BibDatabaseContext database;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private ObservableList<BibEntry> entries;
    private PreferencesService preferences;

    public ImportEntriesViewModel(BackgroundTask<List<BibEntry>> task, TaskExecutor taskExecutor, BibDatabaseContext database, DialogService dialogService, UndoManager undoManager, PreferencesService preferences, StateManager stateManager, FileUpdateMonitor fileUpdateMonitor) {
        this.task = task;
        this.database = database;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entries = FXCollections.observableArrayList();
        this.message = new SimpleStringProperty();
        this.message.bind(task.messageProperty());

        task.onSuccess(entriesToImport -> entries.addAll(entriesToImport))
                .executeWith(taskExecutor);
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
        return findInternalDuplicate(entry).isPresent()
                ||
                new DuplicateCheck(Globals.entryTypesManager).containsDuplicate(database.getDatabase(), entry, database.getMode()).isPresent();
    }

    public void importEntries(List<BibEntry> entriesToImport) {
        // Check if we are supposed to warn about duplicates.
        // If so, then see if there are duplicates, and warn if yes.
        if (preferences.shouldWarnAboutDuplicatesForImport()) {
            BackgroundTask.wrap(() -> entriesToImport.stream()
                    .anyMatch(this::hasDuplicate)).onSuccess(duplicateFound -> {
                if (duplicateFound) {
                    boolean continueImport = dialogService.showConfirmationDialogWithOptOutAndWait(Localization.lang("Duplicates found"),
                            Localization.lang("There are possible duplicates (marked with an icon) that haven't been resolved. Continue?"),
                            Localization.lang("Continue with import"),
                            Localization.lang("Cancel import"),
                            Localization.lang("Disable this confirmation dialog"),
                            optOut -> preferences.setShouldWarnAboutDuplicatesForImport(!optOut));

                    if (!continueImport) {
                        dialogService.notify(Localization.lang("Import canceled"));
                    } else {
                        buildImportHandlerThenImportEntries(entriesToImport);
                    }
                } else {
                    buildImportHandlerThenImportEntries(entriesToImport);
                }
            }).executeWith(Globals.TASK_EXECUTOR);
        } else {
            buildImportHandlerThenImportEntries(entriesToImport);
        }

    }

    private void buildImportHandlerThenImportEntries(List<BibEntry> entriesToImport) {
        ImportHandler importHandler = new ImportHandler(
                dialogService,
                database,
                ExternalFileTypes.getInstance(),
                preferences.getFilePreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getUpdateFieldPreferences(),
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
            if (new DuplicateCheck(Globals.entryTypesManager).isDuplicate(entry, othEntry, database.getMode())) {
                return Optional.of(othEntry);
            }
        }
        return Optional.empty();
    }

    public void resolveDuplicate(BibEntry entry) {
        // First, try to find duplicate in the existing library
        Optional<BibEntry> other = new DuplicateCheck(Globals.entryTypesManager).containsDuplicate(database.getDatabase(), entry, database.getMode());
        if (other.isPresent()) {
            DuplicateResolverDialog dialog = new DuplicateResolverDialog(other.get(),
                    entry, DuplicateResolverDialog.DuplicateResolverType.INSPECTION, database);

            DuplicateResolverDialog.DuplicateResolverResult result = dialog.showAndWait().orElse(DuplicateResolverDialog.DuplicateResolverResult.BREAK);

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
                //entriesToDelete.add(other.get());

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
                    other.get(), DuplicateResolverDialog.DuplicateResolverType.DUPLICATE_SEARCH, database);

            DuplicateResolverDialog.DuplicateResolverResult answer = diag.showAndWait().orElse(DuplicateResolverDialog.DuplicateResolverResult.BREAK);
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
