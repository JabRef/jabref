package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.texparser.CitationViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.texparser.DefaultTexParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.TexParserResult;
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
    private final ObservableList<CitationViewModel> citationList;
    private final ObjectProperty<Status> status;
    private final StringProperty searchError;
    private Future<?> searchTask;
    private TexParserResult texParserResult;

    public LatexCitationsTabViewModel(BibDatabaseContext databaseContext, PreferencesService preferencesService,
                                      TaskExecutor taskExecutor, DialogService dialogService) {
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.directory = new SimpleObjectProperty<>(null);
        this.citationList = FXCollections.observableArrayList();
        this.status = new SimpleObjectProperty<>(Status.IN_PROGRESS);
        this.searchError = new SimpleStringProperty("");
    }

    public void init(BibEntry entry) {
        cancelSearch();

        Optional<String> citeKey = entry.getCiteKeyOptional();
        if (citeKey.isPresent()) {
            startSearch(citeKey.get());
        } else {
            searchError.set(Localization.lang("Selected entry does not have an associated BibTeX key."));
            status.set(Status.ERROR);
        }
    }

    public ObjectProperty<Path> directoryProperty() {
        return directory;
    }

    public ObservableList<CitationViewModel> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public StringProperty searchErrorProperty() {
        return searchError;
    }

    private void startSearch(String citeKey) {
        searchTask = BackgroundTask.wrap(() -> searchAndParse(citeKey))
                                   .onRunning(() -> status.set(Status.IN_PROGRESS))
                                   .onSuccess(status::set)
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

    private Status searchAndParse(String citeKey) throws IOException {
        Path newDirectory = databaseContext.getMetaData().getLaTexFileDirectory(preferencesService.getUser())
                                           .orElseGet(preferencesService::getWorkingDir);

        if (texParserResult == null || !newDirectory.equals(directory.get())) {
            directory.set(newDirectory);

            if (Files.notExists(newDirectory)) {
                throw new IOException(String.format("Current search directory does not exist: %s", newDirectory));
            }

            List<Path> texFiles = searchDirectory(newDirectory, new ArrayList<>());
            texParserResult = new DefaultTexParser().parse(texFiles);
        }

        citationList.setAll(texParserResult.getCitationsByKey(citeKey).stream().map(CitationViewModel::new).collect(Collectors.toList()));

        return citationList.isEmpty() ? Status.NO_RESULTS : Status.CITATIONS_FOUND;
    }

    private List<Path> searchDirectory(Path directory, List<Path> texFiles) {
        Map<Boolean, List<Path>> fileListPartition;
        try (Stream<Path> filesStream = Files.list(directory)) {
            fileListPartition = filesStream.collect(Collectors.partitioningBy(path -> path.toFile().isDirectory()));
        } catch (IOException e) {
            LOGGER.error(String.format("Error searching files: %s", e.getMessage()));
            return texFiles;
        }

        List<Path> subDirectories = fileListPartition.get(true);
        List<Path> files = fileListPartition.get(false)
                                            .stream()
                                            .filter(path -> path.toString().endsWith(TEX_EXT))
                                            .collect(Collectors.toList());
        texFiles.addAll(files);
        subDirectories.forEach(subDirectory -> searchDirectory(subDirectory, texFiles));

        return texFiles;
    }

    public void setLatexDirectory() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(directory.get()).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(selectedDirectory ->
                databaseContext.getMetaData().setLaTexFileDirectory(preferencesService.getUser(), selectedDirectory.toAbsolutePath()));
    }

    public boolean shouldShow() {
        return preferencesService.getEntryEditorPreferences().shouldShowLatexCitationsTab();
    }
}
