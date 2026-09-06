package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.concurrent.Future;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.SciteAiFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.sciteTallies.TalliesResponse;
import org.jabref.model.util.FileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class CitationsRelationsTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationsRelationsTabViewModel.class);

    public enum SciteStatus {
        IN_PROGRESS,
        FOUND,
        ERROR,
        DOI_MISSING,
        DOI_LOOK_UP,
        DOI_LOOK_UP_ERROR
    }

    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;

    private final SciteAiFetcher sciteAiFetcher;

    private final ObjectProperty<@Nullable BibEntry> lastImportedEntry = new SimpleObjectProperty<>();
    private final ObjectProperty<SciteStatus> status;
    private final StringProperty searchError;
    private Optional<TalliesResponse> currentResult = Optional.empty();
    private @Nullable Future<?> searchTask;
    private @Nullable Future<?> doiLookupTask;
    private @Nullable BackgroundTask<List<CitationRelationItem>> citingTask;
    private @Nullable BackgroundTask<List<CitationRelationItem>> citedByTask;
    private @Nullable BibEntry currentEntry;

    public CitationsRelationsTabViewModel(GuiPreferences preferences, StateManager stateManager, DialogService dialogService, FileUpdateMonitor fileUpdateMonitor, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;

        this.status = new SimpleObjectProperty<>(SciteStatus.IN_PROGRESS);
        this.searchError = new SimpleStringProperty("");
        this.sciteAiFetcher = new SciteAiFetcher();
    }

    public void importEntries(List<CitationRelationItem> entriesToImport, CitationFetcher.SearchType searchType, BibEntry existingEntry) {
        assert stateManager.getActiveDatabase().isPresent() : "No active database found, but it is required for importing citation relations";
        Optional<BibDatabaseContext> activeDatabase = stateManager.getActiveDatabase();
        if (activeDatabase.isEmpty()) {
            dialogService.notify(Localization.lang("No library open"));
            return;
        }
        BibDatabaseContext databaseContext = activeDatabase.get();

        assert !entriesToImport.isEmpty() : "No entries to import";
        if (entriesToImport.isEmpty()) {
            return;
        }
        List<BibEntry> entries = entriesToImport.stream()
                                                .map(CitationRelationItem::entry)
                                                // We need to have a clone of the entry, because we add the entry to the library (and keep it in the citation relation tab, too)
                                                .map(BibEntry::new)
                                                .toList();

        ImportHandler importHandler = new ImportHandler(
                databaseContext,
                preferences,
                fileUpdateMonitor,
                stateManager.getUndoManager(databaseContext),
                stateManager,
                dialogService,
                taskExecutor);
        switch (searchType) {
            case CITES ->
                    importCites(entries, existingEntry, importHandler);
            case CITED_BY ->
                    importCitedBy(entries, existingEntry, importHandler);
        }
        lastImportedEntry.set(entries.getFirst());
    }

    private void importCites(List<BibEntry> entries, BibEntry existingEntry, ImportHandler importHandler) {
        importHandler.importEntries(entries);
        // Now, citation keys are set

        SequencedSet<String> citeKeys = existingEntry.getCites();
        entries.stream()
               .flatMap(entry -> entry.getCitationKey().stream())
               .forEach(citeKeys::add);
        existingEntry.setCites(citeKeys);
    }

    /// "cited by" is the opposite of "cites", but not stored in field `CITED_BY`, but in the `CITES` field of the citing entry.
    ///
    /// Therefore, some special handling is needed
    private void importCitedBy(List<BibEntry> entries, BibEntry existingEntry, ImportHandler importHandler) {
        importHandler.importEntries(entries);
        // now the citation keys are set

        if (existingEntry.getCitationKey().isEmpty()) {
            dialogService.notify(Localization.lang("No citation key for %0", existingEntry.getAuthorTitleYear()));
            return;
        }
        String citationKey = existingEntry.getCitationKey().get();
        for (BibEntry citingEntry : entries) {
            SequencedSet<String> existingCites = citingEntry.getCites();
            existingCites.add(citationKey);
            citingEntry.setCites(existingCites);
        }
    }

    public void updateForEntry(@Nullable BibEntry entry) {
        // If a search or lookup is already running, cancel it
        cancelCitationSearches();
        cancelSearch();
        cancelDoiLookup();

        this.currentEntry = entry;

        if (entry == null) {
            searchError.set(Localization.lang("No active entry"));
            status.set(SciteStatus.ERROR);
            return;
        }

        // The scite.ai api requires a DOI
        entry.getDOI().ifPresentOrElse(
                doi -> {
                    status.set(SciteStatus.IN_PROGRESS);
                    searchTask = BackgroundTask.wrap(() -> sciteAiFetcher.fetchTallies(doi))
                                               .onRunning(() -> status.set(SciteStatus.IN_PROGRESS))
                                               .onSuccess(result -> {
                                                   currentResult = Optional.of(result);
                                                   status.set(SciteStatus.FOUND);
                                               })
                                               .onFailure(error -> {
                                                   searchError.set(error.getMessage());
                                                   status.set(SciteStatus.ERROR);
                                               })
                                               .executeWith(taskExecutor);
                },
                () -> status.set(SciteStatus.DOI_MISSING)
        );
    }

    private void cancelSearch() {
        currentResult = Optional.empty();
        status.set(SciteStatus.IN_PROGRESS);

        if (searchTask == null || searchTask.isCancelled() || searchTask.isDone()) {
            return;
        }

        searchTask.cancel(false);
    }

    public void cancelCitationSearches() {
        cancelCitationSearch(CitationFetcher.SearchType.CITES);
        cancelCitationSearch(CitationFetcher.SearchType.CITED_BY);
    }

    public void cancelCitationSearch(CitationFetcher.SearchType searchType) {
        switch (searchType) {
            case CITES -> {
                cancelTrackedCitationSearch(citingTask);
                citingTask = null;
            }
            case CITED_BY -> {
                cancelTrackedCitationSearch(citedByTask);
                citedByTask = null;
            }
        }
    }

    public void trackCitationSearch(CitationFetcher.SearchType searchType, BackgroundTask<List<CitationRelationItem>> task) {
        cancelCitationSearch(searchType);
        switch (searchType) {
            case CITES ->
                    citingTask = task;
            case CITED_BY ->
                    citedByTask = task;
        }
    }

    public boolean isTrackedCitationSearch(CitationFetcher.SearchType searchType, BackgroundTask<List<CitationRelationItem>> task) {
        return switch (searchType) {
            case CITES ->
                    citingTask == task;
            case CITED_BY ->
                    citedByTask == task;
        };
    }

    public void clearTrackedCitationSearch(CitationFetcher.SearchType searchType, BackgroundTask<List<CitationRelationItem>> task) {
        switch (searchType) {
            case CITES -> {
                if (citingTask == task) {
                    citingTask = null;
                }
            }
            case CITED_BY -> {
                if (citedByTask == task) {
                    citedByTask = null;
                }
            }
        }
    }

    private void cancelTrackedCitationSearch(@Nullable BackgroundTask<List<CitationRelationItem>> task) {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
        }
    }

    public void cancelDoiLookup() {
        if (doiLookupTask != null && !doiLookupTask.isCancelled()) {
            doiLookupTask.cancel(false);
            doiLookupTask = null;
        }
    }

    public void lookUpDoi(BibEntry entry, Runnable onDoiFound) {
        cancelDoiLookup();

        CrossRef doiFetcher = new CrossRef(preferences.getImporterPreferences());

        doiLookupTask = BackgroundTask.wrap(() -> doiFetcher.findIdentifier(entry))
                                      .onRunning(() -> status.set(SciteStatus.DOI_LOOK_UP))
                                      .onSuccess(identifier -> {
                                          if (this.currentEntry != entry) {
                                              return;
                                          }
                                          identifier.ifPresentOrElse(
                                                  doi -> {
                                                      entry.setField(StandardField.DOI, doi.asString());
                                                      onDoiFound.run();
                                                  },
                                                  () -> {
                                                      status.set(SciteStatus.DOI_MISSING);
                                                      dialogService.notify(Localization.lang("No DOI found."));
                                                  }
                                          );
                                      })
                                      .onFailure(ex -> {
                                          if (this.currentEntry != entry) {
                                              return;
                                          }
                                          LOGGER.error("Error while looking up DOI", ex);
                                          status.set(SciteStatus.DOI_LOOK_UP_ERROR);
                                          dialogService.notify(Localization.lang("Error while looking up DOI: %0", ex.getLocalizedMessage()));
                                      })
                                      .executeWith(taskExecutor);
    }

    public ObjectProperty<SciteStatus> statusProperty() {
        return status;
    }

    public StringProperty searchErrorProperty() {
        return searchError;
    }

    public Optional<TalliesResponse> getCurrentResult() {
        return currentResult;
    }

    public ReadOnlyObjectProperty<BibEntry> lastImportedEntryProperty() {
        return lastImportedEntry;
    }
}
