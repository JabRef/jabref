package org.jabref.gui.importer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebImportEntriesViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebImportEntriesViewModel.class);
    private static final int PAGE_SIZE = 20;

    public int currentPage = 0;
    public ObservableList<BibEntry> allEntries = FXCollections.observableArrayList();

    private final StringProperty message;
    private final TaskExecutor taskExecutor;
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private ParserResult parserResult = null;
    private final ObservableList<BibEntry> entries;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final ObjectProperty<BibDatabaseContext> selectedDb;
    private final IntegerProperty currentPageProperty = new SimpleIntegerProperty(0);
    private final IntegerProperty totalPagesProperty = new SimpleIntegerProperty(0);
    private final ObservableList<BibEntry> pagedEntries = FXCollections.observableArrayList();
    private final ObservableSet<BibEntry> checkedEntries = FXCollections.observableSet();
    private final SearchBasedFetcher fetcher;
    private final String query;
    private boolean fetchingInProcess;

    /**
     * @param databaseContext the database to import into
     * @param task            the task executed for parsing the selected files(s).
     */
    public WebImportEntriesViewModel(BackgroundTask<ParserResult> task,
                                  TaskExecutor taskExecutor,
                                  BibDatabaseContext databaseContext,
                                  DialogService dialogService,
                                  UndoManager undoManager,
                                  GuiPreferences preferences,
                                  StateManager stateManager,
                                  BibEntryTypesManager entryTypesManager,
                                  FileUpdateMonitor fileUpdateMonitor,
                                  SearchBasedFetcher fetcher,
                                  String query) {
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
        this.selectedDb = new SimpleObjectProperty<>();
        this.fetcher = fetcher;
        this.query = query;
        this.fetchingInProcess = false;

        task.onSuccess(parserResult -> {
            // store the complete parser result (to import groups, ... later on)
            this.parserResult = parserResult;
            // fill in the list for the user, where one can select the entries to import
            entries.addAll(parserResult.getDatabase().getEntries());
            try {
                loadAllEntries(entries);
                updatePagedEntries();
                updateTotalPages();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (entries.isEmpty()) {
                task.updateMessage(Localization.lang("No entries corresponding to given query"));
            }
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

    public ObjectProperty<BibDatabaseContext> selectedDbProperty() {
        return selectedDb;
    }

    public BibDatabaseContext getSelectedDb() {
        return selectedDb.get();
    }

    public ObservableList<BibEntry> getEntries() {
        return pagedEntries;
    }

    public boolean hasDuplicate(BibEntry entry) {
        return findInternalDuplicate(entry).isPresent() ||
                new DuplicateCheck(entryTypesManager)
                        .containsDuplicate(selectedDb.getValue().getDatabase(), entry, selectedDb.getValue().getMode()).isPresent();
    }

    public String getSourceString(BibEntry entry) {
        StringWriter writer = new StringWriter();
        BibWriter bibWriter = new BibWriter(writer, OS.NEWLINE);
        FieldWriter fieldWriter = FieldWriter.buildIgnoreHashes(preferences.getFieldPreferences());
        try {
            new BibEntryWriter(fieldWriter, entryTypesManager).write(entry, bibWriter, selectedDb.getValue().getMode());
        } catch (IOException ioException) {
            return "";
        }
        return writer.toString();
    }

    /**
     * Called after the user selected the entries to import. Does the real import stuff.
     *
     * @param entriesToImport subset of the entries contained in parserResult
     */
    public void importEntries(List<BibEntry> entriesToImport, boolean shouldDownloadFiles) {
        // Remember the selection in the dialog
        preferences.getFilePreferences().setDownloadLinkedFiles(shouldDownloadFiles);

        new DatabaseMerger(preferences.getBibEntryPreferences().getKeywordSeparator()).mergeStrings(
                databaseContext.getDatabase(),
                parserResult.getDatabase());
        new DatabaseMerger(preferences.getBibEntryPreferences().getKeywordSeparator()).mergeMetaData(
                databaseContext.getMetaData(),
                parserResult.getMetaData(),
                parserResult.getPath().map(path -> path.getFileName().toString()).orElse("unknown"),
                parserResult.getDatabase().getEntries());
        ImportHandler importHandler = new ImportHandler(
                selectedDb.getValue(),
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);
        importHandler.importEntriesWithDuplicateCheck(selectedDb.getValue(), entriesToImport);
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

    public ObservableSet<BibEntry> getCheckedEntries() {
        return checkedEntries;
    }

    public ObservableList<BibEntry> getAllEntries() {
        return allEntries;
    }

    public void loadAllEntries(List<BibEntry> entries) {
        allEntries.addAll(entries);
        this.currentPage = 0;
    }

    public void nextPage() {
        if (hasNextPage()) {
            currentPageProperty.set(currentPageProperty.get() + 1);
            currentPage++;
            updatePagedEntries();
            updateTotalPages();
        }
    }

    public void prevPage() {
        if (hasPrevPage()) {
            currentPageProperty.set(currentPageProperty.get() - 1);
            currentPage--;
            updatePagedEntries();
            updateTotalPages();
        }
    }

    public void updateTotalPages() {
        int total = (int) Math.ceil((double) allEntries.size() / PAGE_SIZE);
        totalPagesProperty.set(total);
    }

    public IntegerProperty currentPageProperty() {
        return currentPageProperty;
    }

    public IntegerProperty totalPagesProperty() {
        return totalPagesProperty;
    }

    public boolean hasNextPage() {
        return (currentPage + 1) * PAGE_SIZE < allEntries.size();
    }

    public boolean hasPrevPage() {
        return currentPage > 0;
    }

    private void updatePagedEntries() {
        if (!fetchingInProcess && fetcher.getName().equals("ArXiv") && (currentPage + 1) * PAGE_SIZE >= allEntries.size()) {
            fetchingInProcess = true;
            fetchMoreEntries();
        }

        int fromIdx = currentPage * PAGE_SIZE;
        int toIdx = Math.min(fromIdx + PAGE_SIZE, allEntries.size());
        pagedEntries.setAll(allEntries.subList(fromIdx, toIdx));
    }

    private void fetchMoreEntries() {
        BackgroundTask<ArrayList<BibEntry>> fetchTask = BackgroundTask
            .wrap(() -> {
                ArXivFetcher arXivFetcher = new ArXivFetcher(preferences.getImportFormatPreferences());
                LOGGER.info("Fetching ArXiv entries for page {}", currentPage + 2);
                return new ArrayList<>(arXivFetcher.performSearchPaged(query, currentPage + 1).getContent());
            })
            .onSuccess(newEntries -> {
                if (newEntries != null && !newEntries.isEmpty()) {
                    allEntries.addAll(newEntries);
                    updateTotalPages();
                    fetchingInProcess = false;
                } else {
                    LOGGER.warn("No new entries fetched for page {}", currentPage + 2);
                }
            }).onFailure(exception -> dialogService.showErrorDialogAndWait(
                    Localization.lang("Error fetching entries"),
                    Localization.lang("An error occurred while fetching entries from ArXiv: ") + exception.getMessage()
            ));

        fetchTask.executeWith(taskExecutor);
    }
}
