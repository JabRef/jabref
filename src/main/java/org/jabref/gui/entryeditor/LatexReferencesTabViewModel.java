package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
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

public class LatexReferencesTabViewModel extends AbstractViewModel {

    private static final String TEX_EXT = ".tex";
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final ObjectProperty<BibEntry> entry;
    private final ObservableList<Citation> citationList;
    private final BooleanProperty searchInProgress;
    private final BooleanProperty successfulSearch;

    public LatexReferencesTabViewModel(BibDatabaseContext databaseContext, PreferencesService preferencesService,
                                       TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.entry = new SimpleObjectProperty<>();
        this.citationList = FXCollections.observableArrayList();
        this.searchInProgress = new SimpleBooleanProperty(true);
        this.successfulSearch = new SimpleBooleanProperty(false);
    }

    public void setEntry(BibEntry entry) {
        this.entry.set(entry);
    }

    public ObservableList<Citation> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    public BooleanProperty searchInProgressProperty() {
        return searchInProgress;
    }

    public BooleanProperty successfulSearchProperty() {
        return successfulSearch;
    }

    public void initSearch() {
        BackgroundTask.wrap(this::searchAndParse)
                      .onRunning(() -> {
                          searchInProgress.set(true);
                          successfulSearch.set(false);
                      })
                      .onSuccess(resultsFound -> {
                          successfulSearch.set(resultsFound);
                          searchInProgress.set(false);
                      })
                      .executeWith(taskExecutor);
    }

    private boolean searchAndParse() throws IOException {
        Path directory = databaseContext.getMetaData().getLaTexFileDirectory(preferencesService.getUser())
                                        .orElse(preferencesService.getWorkingDir());

        List<Path> texFiles;
        try (Stream<Path> filesStream = Files.walk(directory)) {
            texFiles = filesStream.filter(path -> path.toFile().isFile() && path.toString().endsWith(TEX_EXT))
                                  .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IOException("An error occurred while searching files: ", e);
        }

        TexParserResult texParserResult = new DefaultTexParser().parse(texFiles);
        Collection<Citation> citationCollection = texParserResult.getCitationsByKey(entry.get());
        citationList.setAll(citationCollection);

        return !citationCollection.isEmpty();
    }

    public boolean shouldShow() {
        return preferencesService.getEntryEditorPreferences().shouldShowLatexReferencesTab();
    }
}
