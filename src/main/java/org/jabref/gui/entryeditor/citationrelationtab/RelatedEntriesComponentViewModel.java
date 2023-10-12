package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.Collections;
import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class RelatedEntriesComponentViewModel {
    private BackgroundTask<List<BibEntry>> task;
    private final BibEntry pivotEntry;
    private final RelatedEntriesRepository repository;
    private final ObjectProperty<Result<List<BibEntry>>> relatedEntriesResultProperty = new SimpleObjectProperty<>();
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final PreferencesService preferencesService;

    public RelatedEntriesComponentViewModel(BibEntry pivotEntry, RelatedEntriesRepository repository, DialogService dialogService, BibDatabaseContext databaseContext, UndoManager undoManager, StateManager stateManager, FileUpdateMonitor fileUpdateMonitor, PreferencesService preferencesService) {
        this.pivotEntry = pivotEntry;
        this.repository = repository;
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.preferencesService = preferencesService;
    }

    /**
     * Load related entries from cache if available, otherwise, load from remote server.
     * */
    public void loadEntries() {
        load(false);
    }

    /**
     * This method, unlike {@link #loadEntries()}, will always load related entries from remote server even when they are
     * available in cache.
     * */
    public void reloadEntries() {
        load(true);
    }

    /**
     * Cancels the ongoing related entries loading task
     * */
    public void cancelLoading() {
        relatedEntriesResultProperty.set(Result.success(Collections.emptyList()));
        if (task != null) {
            task.cancel();
        }
    }

    public ObjectProperty<Result<List<BibEntry>>> relatedEntriesResultPropertyProperty() {
        return relatedEntriesResultProperty;
    }

    private void load(boolean shouldRefresh) {
        task = BackgroundTask.wrap(() -> {
                                 if (shouldRefresh) {
                                     repository.refreshCache(pivotEntry);
                                 }
                                 return repository.lookupRelatedEntries(pivotEntry);
                             })
                             .onRunning(() -> relatedEntriesResultProperty.set(Result.pending()))
                             .onSuccess(entries -> relatedEntriesResultProperty.set(Result.success(entries)))
                             .onFailure(e -> relatedEntriesResultProperty.set(Result.failure(e)));

        task.executeWith(Globals.TASK_EXECUTOR);
    }

    public void importEntriesIntoCurrentLibrary(List<CitationRelationItem> entriesToImport) {
        List<BibEntry> entries = entriesToImport.stream().map(CitationRelationItem::getEntry).toList();
        ImportHandler importHandler = new ImportHandler(
                databaseContext,
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                Globals.TASK_EXECUTOR);

        importHandler.importEntries(entries);

        dialogService.notify(Localization.lang("Number of entries successfully imported") + ": " + entriesToImport.size());
    }
}
