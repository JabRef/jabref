package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.texparser.DefaultTexParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.TexParserResult;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LatexCitationsTabViewModel extends AbstractViewModel {

    enum Status {
        INACTIVE,
        IN_PROGRESS,
        CITATIONS_FOUND,
        NO_RESULTS,
        ERROR
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LatexCitationsTabViewModel.class);

    private static final String TEX_EXT = ".tex";
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final ObjectProperty<BibEntry> entry;
    private final ObservableList<Citation> citationList;
    private final ObjectProperty<Status> status;
    private Exception searchError;
    private Future<?> searchTask;

    public LatexCitationsTabViewModel(BibDatabaseContext databaseContext, PreferencesService preferencesService,
                                      TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.entry = new SimpleObjectProperty<>(null);
        this.citationList = FXCollections.observableArrayList();
        this.status = new SimpleObjectProperty<>(Status.INACTIVE);
    }

    public void init(BibEntry entry) {
        cancelSearch();
        this.entry.set(entry);
        startSearch();
    }

    public ObservableList<Citation> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public Exception getSearchError() {
        return searchError;
    }

    private void startSearch() {
        searchTask = BackgroundTask.wrap(this::searchAndParse)
                                   .onRunning(() -> status.set(Status.IN_PROGRESS))
                                   .onSuccess(status::set)
                                   .onFailure(error -> {
                                       searchError = error;
                                       status.set(Status.ERROR);
                                   })
                                   .executeWith(taskExecutor);
    }

    private void cancelSearch() {
        if (searchTask == null || searchTask.isCancelled() || searchTask.isDone()) {
            return;
        }

        searchTask.cancel(true);

        if (searchTask.isCancelled()) {
            LOGGER.debug("Last search has been cancelled");
        } else {
            LOGGER.warn("Could not cancel last search");
        }
    }

    private Status searchAndParse() throws IOException {
        Path directory = databaseContext.getMetaData().getLaTexFileDirectory(preferencesService.getUser())
                                        .orElse(preferencesService.getWorkingDir());

        List<Path> texFiles;
        try (Stream<Path> filesStream = Files.walk(directory)) {
            texFiles = filesStream.filter(path -> path.toFile().isFile() && path.toString().endsWith(TEX_EXT))
                                  .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error searching files", e);
            throw new IOException("Error searching files", e);
        }

        TexParserResult texParserResult = new DefaultTexParser().parse(entry.get(), texFiles);
        citationList.setAll(texParserResult.getCitationsByKey(entry.get()));

        return citationList.isEmpty() ? Status.NO_RESULTS : Status.CITATIONS_FOUND;
    }

    public boolean shouldShow() {
        return preferencesService.getEntryEditorPreferences().shouldShowLatexCitationsTab();
    }
}
