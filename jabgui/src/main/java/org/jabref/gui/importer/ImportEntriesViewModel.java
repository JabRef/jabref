package org.jabref.gui.importer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportEntriesViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportEntriesViewModel.class);
    private static final int PAGE_SIZE = 20;

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
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty initialLoadComplete = new SimpleBooleanProperty(false);
    private final ObservableList<BibEntry> pagedEntries = FXCollections.observableArrayList();
    private final ObservableSet<BibEntry> checkedEntries = FXCollections.observableSet();
    private final ObservableList<BibEntry> allEntries = FXCollections.observableArrayList();
    private final Optional<SearchBasedFetcher> fetcher;
    private final Optional<String> query;

    /**
     * @param databaseContext the database to import into
     * @param task            the task executed for parsing the selected files(s).
     */
    public ImportEntriesViewModel(BackgroundTask<ParserResult> task,
                                  TaskExecutor taskExecutor,
                                  BibDatabaseContext databaseContext,
                                  DialogService dialogService,
                                  UndoManager undoManager,
                                  GuiPreferences preferences,
                                  StateManager stateManager,
                                  BibEntryTypesManager entryTypesManager,
                                  FileUpdateMonitor fileUpdateMonitor,
                                  Optional<SearchBasedFetcher> fetcher,
                                  Optional<String> query) {
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

        task.onSuccess(parserResult -> {
            // store the complete parser result (to import groups, ... later on)
            this.parserResult = parserResult;
            // fill in the list for the user, where one can select the entries to import
            entries.addAll(parserResult.getDatabase().getEntries());
            loadEntries(entries);
            updatePagedEntries();
            updateTotalPages();
            initialLoadComplete.set(true);
            if (entries.isEmpty()) {
                task.updateMessage(Localization.lang("No entries corresponding to given query"));
            }
        }).onFailure(ex -> {
            LOGGER.error("Error importing", ex);
            initialLoadComplete.set(true);
            dialogService.showErrorDialogAndWait(ex);
        }).executeWith(taskExecutor);
    }

    public BooleanProperty initialLoadCompleteProperty() {
        return initialLoadComplete;
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

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public ObservableList<BibEntry> getEntries() {
        return pagedEntries;
    }

    public ObservableSet<BibEntry> getCheckedEntries() {
        return checkedEntries;
    }

    public ObservableList<BibEntry> getAllEntries() {
        return allEntries;
    }

    public void loadEntries(List<BibEntry> entries) {
        allEntries.addAll(entries);
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
            // Force reformatting so the displayed BibTeX is consistently formatted
            new BibEntryWriter(fieldWriter, entryTypesManager).write(entry, bibWriter, selectedDb.getValue().getMode(), true);
        } catch (IOException ioException) {
            // In case of error, fall back to the original parsed serialization if available
            return entry.getParsedSerialization();
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
        importHandler.importEntriesWithDuplicateCheck(null, entriesToImport);

        // Merge groups from imported library
        if (parserResult != null && selectedDb != null) {
            mergeGroupsFromImport(parserResult, selectedDb);
            
            // Trigger UI refresh
            stateManager.getActiveDatabase().ifPresent(db -> {
                db.getMetaData().getGroups().ifPresent(rootNode -> {
                    db.getMetaData().setGroups(rootNode);
                });
            });
        }
    }

    /**
     * Merges groups from imported library into target library.
     * If target has no groups, copies the entire group tree from import.
     * If both have groups, merges them recursively.
     *
     * @param importedResult the parser result containing imported data with groups
     * @param targetContext  the target database context to merge groups into
     */
    private void mergeGroupsFromImport(ParserResult importedResult, 
                                       BibDatabaseContext targetContext) {
        if (importedResult.getMetaData().getGroups().isPresent()
                && targetContext.getMetaData().getGroups().isPresent()) {
            
            GroupTreeNode importedRoot = importedResult.getMetaData().getGroups().get();
            GroupTreeNode targetRoot = targetContext.getMetaData().getGroups().get();
            
            mergeGroupTrees(importedRoot, targetRoot);
            targetContext.getMetaData().setGroups(targetRoot);
        } else if (importedResult.getMetaData().getGroups().isPresent()
                && !targetContext.getMetaData().getGroups().isPresent()) {
            
            GroupTreeNode importedRoot = importedResult.getMetaData().getGroups().get();
            targetContext.getMetaData().setGroups(importedRoot.copySubtree());
        }
    }

    /**
     * Recursively merges group trees.
     * Groups with same name are merged, new groups are added.
     *
     * @param source the source group tree node to merge from
     * @param target the target group tree node to merge into
     */
    private void mergeGroupTrees(GroupTreeNode source, GroupTreeNode target) {
        for (GroupTreeNode sourceChild : source.getChildren()) {
            Optional<GroupTreeNode> existingGroup = target.getChildren().stream()
                    .filter(child -> child.getGroup().getName()
                            .equals(sourceChild.getGroup().getName()))
                    .findFirst();
            
            if (existingGroup.isPresent()) {
                mergeGroupTrees(sourceChild, existingGroup.get());
            } else {
                target.addChild(sourceChild.copySubtree());
            }
        }
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

    public void goToPrevPage() {
        if (hasPrevPage()) {
            currentPageProperty.set(currentPageProperty.get() - 1);
            updatePagedEntries();
        }
    }

    public void goToNextPage() {
        if (hasNextPage()) {
            currentPageProperty.set(currentPageProperty.get() + 1);
            updatePagedEntries();
        }
    }

    public boolean hasNextPage() {
        return (currentPageProperty.get() + 1) * PAGE_SIZE < allEntries.size();
    }

    public boolean hasPrevPage() {
        return currentPageProperty.get() > 0;
    }

    public IntegerProperty currentPageProperty() {
        return currentPageProperty;
    }

    public IntegerProperty totalPagesProperty() {
        return totalPagesProperty;
    }

    public void updateTotalPages() {
        int total = (int) Math.ceil((double) allEntries.size() / PAGE_SIZE);
        totalPagesProperty.set(total);
    }

    private boolean isFromWebSearch() {
        return fetcher.isPresent() && query.isPresent();
    }

    private void updatePagedEntries() {
        if (!isFromWebSearch()) {
            // For entries other than web search, show all entries at once
            pagedEntries.setAll(allEntries);
            return;
        }

        int fromIdx = currentPageProperty.get() * PAGE_SIZE;
        int toIdx = Math.min(fromIdx + PAGE_SIZE, allEntries.size());
        pagedEntries.setAll(allEntries.subList(fromIdx, toIdx));
    }

    public void fetchMoreEntries() {
        if (fetcher.isPresent() &&
                fetcher.get() instanceof PagedSearchBasedFetcher pagedFetcher &&
                query.isPresent() && !loading.get()) {
            loading.set(true);
            BackgroundTask<ArrayList<BibEntry>> fetchTask = BackgroundTask
                    .wrap(() -> {
                        LOGGER.info("Fetching entries from {} for page {}", fetcher.get().getName(), currentPageProperty.get() + 2);
                        return new ArrayList<>(pagedFetcher.performSearchPaged(query.get(), currentPageProperty.get() + 1).getContent());
                    })
                    .onSuccess(newEntries -> {
                        if (newEntries != null && !newEntries.isEmpty()) {
                            allEntries.addAll(newEntries);
                            updateTotalPages();
                        } else {
                            LOGGER.warn("No new entries fetched from {} for page {}", fetcher.get().getName(), currentPageProperty.get() + 2);
                            dialogService.notify(Localization.lang("No new entries found from %0", fetcher.get().getName()));
                        }
                        loading.set(false);
                    })
                    .onFailure(exception -> {
                        loading.set(false);
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("Error fetching entries"),
                                Localization.lang("An error occurred while fetching entries from %0: %1",
                                        fetcher.get().getName(), exception.getMessage())
                        );
                    });

            fetchTask.executeWith(taskExecutor);
        }
    }
}
