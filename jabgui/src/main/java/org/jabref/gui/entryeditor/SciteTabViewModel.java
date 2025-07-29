package org.jabref.gui.entryeditor;

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
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import kong.unirest.core.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SciteTabViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(SciteTabViewModel.class);

    public enum SciteStatus {
        IN_PROGRESS,
        FOUND,
        ERROR,
        DOI_MISSING,
        DOI_LOOK_UP,
        DOI_LOOK_UP_ERROR
    }

    private static final String BASE_URL = "https://api.scite.ai/";

    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final ObjectProperty<SciteStatus> status;
    private final StringProperty searchError;
    private Optional<SciteTallyModel> currentResult = Optional.empty();
    private Future<?> searchTask;

    public SciteTabViewModel(GuiPreferences preferences, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.status = new SimpleObjectProperty<>(SciteStatus.IN_PROGRESS);
        this.searchError = new SimpleStringProperty("");
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
        URL url;
        try {
            url = new URI(BASE_URL + "tallies/" + doi.asString()).toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            throw new FetcherException("Malformed URL for DOI", ex);
        }
        LOGGER.debug("Fetching tallies from {}", url);
        URLDownload download = new URLDownload(url);
        String response = download.asString();
        LOGGER.debug("Response {}", response);
        JSONObject tallies = new JSONObject(response);
        if (tallies.has("detail")) {
            String message = tallies.getString("detail");
            throw new FetcherException(message);
        } else if (!tallies.has("total")) {
            throw new FetcherException("Unexpected result data.");
        }
        return SciteTallyModel.fromJSONObject(tallies);
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

    public Optional<SciteTallyModel> getCurrentResult() {
        return currentResult;
    }
}
