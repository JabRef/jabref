package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.concurrent.Future;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.ScienceAiFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.sciteTallies.TalliesResponse;
import org.jabref.model.util.FileUpdateMonitor;

public class CitationsRelationsTabViewModel {

    public enum SciteStatus {
        IN_PROGRESS,
        FOUND,
        ERROR,
        DOI_MISSING,
        DOI_LOOK_UP,
        DOI_LOOK_UP_ERROR
    }

    private final GuiPreferences preferences;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;

    private final ScienceAiFetcher scienceAiFetcher;

    private final ObjectProperty<SciteStatus> status;
    private final StringProperty searchError;
    private Optional<TalliesResponse> currentResult = Optional.empty();
    private Future<?> searchTask;

    public CitationsRelationsTabViewModel(GuiPreferences preferences, UndoManager undoManager, StateManager stateManager, DialogService dialogService, FileUpdateMonitor fileUpdateMonitor, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;

        this.status = new SimpleObjectProperty<>(SciteStatus.IN_PROGRESS);
        this.searchError = new SimpleStringProperty("");
        this.scienceAiFetcher = new ScienceAiFetcher();
    }

    public void importEntries(List<CitationRelationItem> entriesToImport, CitationFetcher.SearchType searchType, BibEntry existingEntry) {
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().orElse(BibDatabaseContext.empty());

        List<BibEntry> entries = entriesToImport.stream()
                                                .map(CitationRelationItem::entry)
                                                // We need to have a clone of the entry, because we add the entry to the library (and keep it in the citation relation tab, too)
                                                .map(BibEntry::new)
                                                .toList();

        ImportHandler importHandler = new ImportHandler(
                databaseContext,
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);
        CitationKeyGenerator generator = new CitationKeyGenerator(databaseContext, preferences.getCitationKeyPatternPreferences());
        boolean generateNewKeyOnImport = preferences.getImporterPreferences().generateNewKeyOnImportProperty().get();

        switch (searchType) {
            case CITES ->
                    importCites(entries, existingEntry, importHandler, generator, generateNewKeyOnImport);
            case CITED_BY ->
                    importCitedBy(entries, existingEntry, importHandler, generator, generateNewKeyOnImport);
        }
    }

    private void importCites(List<BibEntry> entries, BibEntry existingEntry, ImportHandler importHandler, CitationKeyGenerator generator, boolean generateNewKeyOnImport) {
        SequencedSet<String> citeKeys = existingEntry.getCites();

        for (BibEntry entryToCite : entries) {
            if (generateNewKeyOnImport || entryToCite.getCitationKey().isEmpty()) {
                String key = generator.generateKey(entryToCite);
                entryToCite.setCitationKey(key);
            }
            citeKeys.add(entryToCite.getCitationKey().get());
        }

        existingEntry.setCites(citeKeys);
        importHandler.importEntries(entries);
    }

    /**
     * "cited by" is the opposite of "cites", but not stored in field `CITED_BY`, but in the `CITES` field of the citing entry.
     * <p>
     * Therefore, some special handling is needed
     */
    private void importCitedBy(List<BibEntry> entries, BibEntry existingEntry, ImportHandler importHandler, CitationKeyGenerator generator, boolean generateNewKeyOnImport) {
        if (existingEntry.getCitationKey().isEmpty()) {
            if (!generateNewKeyOnImport) {
                dialogService.notify(Localization.lang("No citation key for %0", existingEntry.getAuthorTitleYear()));
                return;
            }
            existingEntry.setCitationKey(generator.generateKey(existingEntry));
        }
        String citationKey = existingEntry.getCitationKey().get();

        for (BibEntry citingEntry : entries) {
            SequencedSet<String> existingCites = citingEntry.getCites();
            existingCites.add(citationKey);
            citingEntry.setCites(existingCites);
        }

        importHandler.importEntries(entries);
    }

    public boolean shouldShow() {
        return preferences.getEntryEditorPreferences().shouldShowSciteTab();
    }

    public void bindToEntry(BibEntry entry) {
        // If a search is already running, cancel it
        cancelSearch();

        if (entry == null) {
            searchError.set(Localization.lang("No active entry"));
            status.set(SciteStatus.ERROR);
            return;
        }

        // The scite.ai api requires a DOI
        if (entry.getDOI().isEmpty()) {
            status.set(SciteStatus.DOI_MISSING);
            return;
        }

        searchTask = BackgroundTask.wrap(() -> scienceAiFetcher.fetchTallies(entry.getDOI().get()))
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
    }

    private void cancelSearch() {
        if (searchTask == null || searchTask.isCancelled() || searchTask.isDone()) {
            return;
        }

        status.set(SciteStatus.IN_PROGRESS);
        searchTask.cancel(true);
    }

    public void lookUpDoi(BibEntry entry) {
        CrossRef doiFetcher = new CrossRef();

        BackgroundTask.wrap(() -> doiFetcher.findIdentifier(entry))
                      .onRunning(() -> {
                          status.set(SciteStatus.DOI_LOOK_UP);
                      })
                      .onSuccess(identifier -> {
                          if (identifier.isPresent()) {
                              entry.setField(StandardField.DOI, identifier.get().asString());
                              bindToEntry(entry);
                          } else {
                              status.set(SciteStatus.DOI_MISSING);
                          }
                      }).onFailure(ex -> {
                          status.set(SciteStatus.DOI_LOOK_UP_ERROR);
                      }).executeWith(taskExecutor);
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
}
