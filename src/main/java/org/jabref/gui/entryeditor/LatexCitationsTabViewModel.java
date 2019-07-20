package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(LatexCitationsTabViewModel.class);

    private static final String TEX_EXT = ".tex";
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final ObjectProperty<BibEntry> entry;
    private final ObservableList<Citation> citationList;
    private final BooleanProperty workingInProgress;
    private final BooleanProperty successfulSearch;
    private final BooleanProperty notFoundResults;
    private final ObjectProperty<Exception> searchError;
    private Future<?> searchTask;

    public LatexCitationsTabViewModel(BibDatabaseContext databaseContext, PreferencesService preferencesService,
                                      TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;

        this.entry = new SimpleObjectProperty<>(null);
        this.citationList = FXCollections.observableArrayList();
        this.workingInProgress = new SimpleBooleanProperty(true);
        this.successfulSearch = new SimpleBooleanProperty(false);
        this.notFoundResults = new SimpleBooleanProperty(false);
        this.searchError = new SimpleObjectProperty<>(null);
    }

    public void init(BibEntry entry) {
        cancelSearch();
        this.workingInProgress.set(true);
        this.citationList.clear();
        this.entry.set(entry);
        startSearch();
    }

    public ObservableList<Citation> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    public BooleanProperty workingInProgressProperty() {
        return workingInProgress;
    }

    public BooleanProperty successfulSearchProperty() {
        return successfulSearch;
    }

    public BooleanProperty notFoundResultsProperty() {
        return notFoundResults;
    }

    public ObjectProperty<Exception> searchErrorProperty() {
        return searchError;
    }

    private void startSearch() {
        searchTask = BackgroundTask.wrap(this::searchAndParse)
                                   .onRunning(() -> {
                                       workingInProgress.set(true);
                                       successfulSearch.set(false);
                                       notFoundResults.set(false);
                                       searchError.set(null);
                                   })
                                   .onFailure(searchError::set)
                                   .onSuccess(noResults -> {
                                       workingInProgress.set(false);
                                       successfulSearch.set(!noResults);
                                       notFoundResults.set(noResults);
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

    private boolean searchAndParse() throws IOException {
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

        TexParserResult texParserResult = new DefaultTexParser().parse(texFiles);
        citationList.setAll(texParserResult.getCitationsByKey(entry.get()));

        return citationList.isEmpty();
    }

    public boolean shouldShow() {
        return preferencesService.getEntryEditorPreferences().shouldShowLatexCitationsTab();
    }
}
