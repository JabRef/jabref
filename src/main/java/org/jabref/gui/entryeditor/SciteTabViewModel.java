package org.jabref.gui.entryeditor;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.Future;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.preferences.PreferencesService;

import kong.unirest.json.JSONObject;
import org.tinylog.Logger;

public class SciteTabViewModel extends AbstractViewModel {

    // The view only has three states - In Progress, found (success) and error
    enum Status {
        IN_PROGRESS,
        FOUND,
        ERROR
    }

    private static final String BASE_URL = "https://api.scite.ai/";

    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final ObjectProperty<Status> status;
    private final StringProperty searchError;
    private Optional<SciteTallyDTO> currentResult = Optional.empty();

    private Future<?> searchTask;

    public SciteTabViewModel(PreferencesService preferencesService, TaskExecutor taskExecutor) {
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.status = new SimpleObjectProperty<>(Status.IN_PROGRESS);
        this.searchError = new SimpleStringProperty("");
    }

    public boolean shouldShow() {
        return preferencesService.getEntryEditorPreferences().shouldShowSciteTab();
    }

    public void bindToEntry(BibEntry entry) {
        // If a search is already running, cancel it
        cancelSearch();

        if (entry == null) {
            searchError.set("Null Entry!");
            status.set(Status.ERROR);
            return;
        }

        // The scite.ai api requires a DOI
        if (entry.getDOI().isEmpty()) {
            searchError.set("This entry does not have a DOI");
            status.set(Status.ERROR);
            return;
        }

        searchTask = BackgroundTask.wrap(() -> fetchTallies(entry.getDOI().get()))
            .onRunning(() -> status.set(Status.IN_PROGRESS))
            .onSuccess(result -> {
                currentResult = Optional.of(result);
                status.set(Status.FOUND);
            })
            .onFailure(error -> {
                searchError.set(error.getMessage());
                status.set(Status.ERROR);
            })
            .executeWith(taskExecutor);
    }

    private void cancelSearch() {
        if (searchTask == null || searchTask.isCancelled() || searchTask.isDone()) {
            return;
        }

        status.set(Status.IN_PROGRESS);
        searchTask.cancel(true);
    }

    public SciteTallyDTO fetchTallies(DOI doi) {
        try {
            URL url = new URL(BASE_URL + "tallies/" + doi.getDOI());
            URLDownload download = new URLDownload(url);
            String response = download.asString();
            Logger.debug("Response {}", response);
            JSONObject tallies = new JSONObject(response);
            if (tallies.has("detail")) {
                String message = tallies.getString("detail");
                throw new RuntimeException(message);
            } else if (!tallies.has("total")) {
                throw new RuntimeException("Unexpected result data!");
            }

            return SciteTallyDTO.fromJSONObject(tallies);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public StringProperty searchErrorProperty() {
        return searchError;
    }

    public Optional<SciteTallyDTO> getCurrentResult() {
        return currentResult;
    }
}
