package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.preferences.PreferencesService;

import kong.unirest.json.JSONObject;
import org.tinylog.Logger;

public class SciteTabViewModel extends AbstractViewModel {

    /**
     * Status enum for Scite tab
     */
    public enum SciteStatus {
        IN_PROGRESS,
        FOUND,
        ERROR
    }

    private static final String BASE_URL = "https://api.scite.ai/";
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final ObjectProperty<SciteStatus> status;
    private final StringProperty searchError;
    private Optional<SciteTallyModel> currentResult = Optional.empty();

    private Future<?> searchTask;

    public SciteTabViewModel(PreferencesService preferencesService, TaskExecutor taskExecutor) {
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.status = new SimpleObjectProperty<>(SciteStatus.IN_PROGRESS);
        this.searchError = new SimpleStringProperty("");
    }

    public boolean shouldShow() {
        return preferencesService.getEntryEditorPreferences().shouldShowSciteTab();
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
            searchError.set(Localization.lang("This entry does not have a DOI"));
            status.set(SciteStatus.ERROR);
            return;
        }

        searchTask = BackgroundTask.wrap(() -> fetchTallies(entry.getDOI().get()))
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

    public SciteTallyModel fetchTallies(DOI doi) throws FetcherException {
        try {
            URL url = new URI(BASE_URL + "tallies/" + doi.getDOI()).toURL();
            URLDownload download = new URLDownload(url);
            String response = download.asString();
            Logger.debug("Response {}", response);
            JSONObject tallies = new JSONObject(response);
            if (tallies.has("detail")) {
                String message = tallies.getString("detail");
                throw new FetcherException(message);
            } else if (!tallies.has("total")) {
                throw new FetcherException("Unexpected result data!");
            }
            return SciteTallyModel.fromJSONObject(tallies);
        } catch (MalformedURLException | URISyntaxException ex) {
            throw new FetcherException("Malformed url for DOs", ex);
        } catch (IOException ioex) {
            throw new FetcherException("Failed to retrieve tallies for DOI - IO Exception", ioex);
        }
    }

    public ObjectProperty<SciteStatus> statusProperty() {
        return status;
    }

    public StringProperty searchErrorProperty() {
        return searchError;
    }

    public Optional<SciteTallyModel> getCurrentResult() {
        return currentResult;
    }
}
