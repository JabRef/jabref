package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.Globals;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.model.entry.BibEntry;

public class RelatedEntriesComponentViewModel {
    private BackgroundTask<List<BibEntry>> task;
    private final BibEntry pivotEntry;
    private final RelatedEntriesRepository repository;
    private final ObjectProperty<Result<List<BibEntry>>> relatedEntriesResultProperty = new SimpleObjectProperty<>();

    public RelatedEntriesComponentViewModel(BibEntry pivotEntry, RelatedEntriesRepository repository) {
        this.pivotEntry = pivotEntry;
        this.repository = repository;
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
                                 if (pivotEntry.getDOI().isEmpty()) {
                                     throw new IllegalArgumentException("The selected entry doesn't have a DOI linked to it. Lookup a DOI and try again.");
                                 }
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
}
