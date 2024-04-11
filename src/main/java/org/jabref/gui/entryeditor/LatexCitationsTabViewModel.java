package org.jabref.gui.entryeditor;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Future;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.texparser.CitationFinder;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.Citation;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LatexCitationsTabViewModel extends AbstractViewModel {

    enum Status {
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
    private final DialogService dialogService;
    private final ObjectProperty<Path> directory;
    private CitationFinder citationFinder;
    private final ObservableList<Citation> citationList;
    private final ObjectProperty<Status> status;
    private final StringProperty searchError;
    private Future<?> searchTask;
    private BibEntry currentEntry;

    public LatexCitationsTabViewModel(BibDatabaseContext databaseContext,
                                      PreferencesService preferencesService,
                                      TaskExecutor taskExecutor,
                                      DialogService dialogService) {
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.directory = new SimpleObjectProperty<>(databaseContext.getMetaData().getLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost())
                                                                   .orElse(FileUtil.getInitialDirectory(databaseContext, preferencesService.getFilePreferences().getWorkingDirectory())));

        this.citationFinder = new CitationFinder(directory.get());
        this.citationList = FXCollections.observableArrayList();
        this.status = new SimpleObjectProperty<>(Status.IN_PROGRESS);
        this.searchError = new SimpleStringProperty("");
    }

    public void init(BibEntry entry) {
        cancelSearch();

        currentEntry = entry;
        Optional<String> citeKey = entry.getCitationKey();

        if (citeKey.isPresent()) {
            startSearch(citeKey.get());
        } else {
            searchError.set(Localization.lang("Selected entry does not have an associated citation key."));
            status.set(Status.ERROR);
        }
    }

    public ObjectProperty<Path> directoryProperty() {
        return directory;
    }

    public ObservableList<Citation> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public StringProperty searchErrorProperty() {
        return searchError;
    }

    private void startSearch(String citeKey) {
        // we need to check whether the user meanwhile set the LaTeX file directory or the database changed locations
        checkAndUpdateDirectory();

        searchTask = BackgroundTask.wrap(() -> citationFinder.searchAndParse(citeKey))
                                   .onRunning(() -> status.set(Status.IN_PROGRESS))
                                   .onSuccess(result -> {
                                       citationList.setAll(result);
                                       status.set(citationList.isEmpty() ? Status.NO_RESULTS : Status.CITATIONS_FOUND);
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

    public void checkAndUpdateDirectory() {
        Path newDirectory = databaseContext.getMetaData().getLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost())
                                           .orElse(FileUtil.getInitialDirectory(databaseContext, preferencesService.getFilePreferences().getWorkingDirectory()));

        if (!newDirectory.equals(directory.get())) {
            directory.set(newDirectory);
            citationFinder = new CitationFinder(newDirectory);
        }
    }

    public void setLatexDirectory() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(directory.get()).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(selectedDirectory ->
                databaseContext.getMetaData().setLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost(), selectedDirectory.toAbsolutePath()));

        init(currentEntry);
    }

    public void refreshLatexDirectory() {
        init(currentEntry);
    }

    public boolean shouldShow() {
        return preferencesService.getEntryEditorPreferences().shouldShowLatexCitationsTab();
    }
}
