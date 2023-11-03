package org.jabref.gui.entryeditor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import kong.unirest.json.JSONObject;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.preferences.PreferencesService;
import org.tinylog.Logger;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.Future;

public class SciteTabViewModel extends AbstractViewModel {

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
        cancelSearch();

        if (entry == null) {
            searchError.set("Null Entry!");
            status.set(Status.ERROR);
            return;
        }

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

    public static class SciteTallyDTO {

        private String doi;
        private int total;
        private int supporting;
        private int contradicting;
        private int mentioning;
        private int unclassified;
        private int citingPublications;

        public static SciteTallyDTO fromJSONObject(JSONObject jsonObject) {
            SciteTallyDTO dto = new SciteTallyDTO();

            dto.setDoi(jsonObject.getString("doi"));
            dto.setTotal(jsonObject.getInt("total"));
            dto.setSupporting(jsonObject.getInt("supporting"));
            dto.setContradicting(jsonObject.getInt("contradicting"));
            dto.setMentioning(jsonObject.getInt("mentioning"));
            dto.setUnclassified(jsonObject.getInt("unclassified"));
            dto.setCitingPublications(jsonObject.getInt("citingPublications"));
            return dto;
        }

        public String getDoi() {
            return doi;
        }

        public void setDoi(String doi) {
            this.doi = doi;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSupporting() {
            return supporting;
        }

        public void setSupporting(int supporting) {
            this.supporting = supporting;
        }

        public int getContradicting() {
            return contradicting;
        }

        public void setContradicting(int contradicting) {
            this.contradicting = contradicting;
        }

        public int getMentioning() {
            return mentioning;
        }

        public void setMentioning(int mentioning) {
            this.mentioning = mentioning;
        }

        public int getUnclassified() {
            return unclassified;
        }

        public void setUnclassified(int unclassified) {
            this.unclassified = unclassified;
        }

        public int getCitingPublications() {
            return citingPublications;
        }

        public void setCitingPublications(int citingPublications) {
            this.citingPublications = citingPublications;
        }

    }

}
