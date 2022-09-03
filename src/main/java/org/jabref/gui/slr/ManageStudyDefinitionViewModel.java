package org.jabref.gui.slr;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.ACMPortalFetcher;
import org.jabref.logic.importer.fetcher.CompositeSearchBasedFetcher;
import org.jabref.logic.importer.fetcher.DBLPFetcher;
import org.jabref.logic.importer.fetcher.IEEE;
import org.jabref.logic.importer.fetcher.SpringerFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;
import org.jabref.model.study.StudyQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a model for managing study definitions.
 * To visualize the model one can bind the properties to UI elements.
 */
public class ManageStudyDefinitionViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManageStudyDefinitionViewModel.class);

    private static final Set<String> DEFAULT_SELECTION = Set.of(
            ACMPortalFetcher.FETCHER_NAME,
            IEEE.FETCHER_NAME,
            SpringerFetcher.FETCHER_NAME,
            DBLPFetcher.FETCHER_NAME);


    private final StringProperty title = new SimpleStringProperty();
    private final ObservableList<String> authors = FXCollections.observableArrayList();
    private final ObservableList<String> researchQuestions = FXCollections.observableArrayList();
    private final ObservableList<String> queries = FXCollections.observableArrayList();
    private final ObservableList<StudyDatabaseItem> databases = FXCollections.observableArrayList();

    // Hold the complement of databases for the selector
    private final SimpleStringProperty directory = new SimpleStringProperty();

    private final DialogService dialogService;

    /**
     * Constructor for a new study
     */
    public ManageStudyDefinitionViewModel(ImportFormatPreferences importFormatPreferences,
                                          ImporterPreferences importerPreferences,
                                          DialogService dialogService) {
        databases.addAll(WebFetchers.getSearchBasedFetchers(importFormatPreferences, importerPreferences)
                                    .stream()
                                    .map(SearchBasedFetcher::getName)
                                    // The user wants to select specific fetchers
                                    // The fetcher summarizing ALL fetchers can be emulated by selecting ALL fetchers (which happens rarely when doing an SLR)
                                    .filter(name -> !name.equals(CompositeSearchBasedFetcher.FETCHER_NAME))
                                    .map(name -> {
                                        boolean enabled = DEFAULT_SELECTION.contains(name);
                                        return new StudyDatabaseItem(name, enabled);
                                    })
                                    .toList());
        this.dialogService = Objects.requireNonNull(dialogService);
    }

    /**
     * Constructor for an existing study
     *
     * @param study The study to initialize the UI from
     * @param studyDirectory The path where the study resides
     */
    public ManageStudyDefinitionViewModel(Study study,
                                          Path studyDirectory,
                                          ImportFormatPreferences importFormatPreferences,
                                          ImporterPreferences importerPreferences,
                                          DialogService dialogService) {
        // copy the content of the study object into the UI fields
        authors.addAll(Objects.requireNonNull(study).getAuthors());
        title.setValue(study.getTitle());
        researchQuestions.addAll(study.getResearchQuestions());
        queries.addAll(study.getQueries().stream().map(StudyQuery::getQuery).toList());
        List<StudyDatabase> studyDatabases = study.getDatabases();
        databases.addAll(WebFetchers.getSearchBasedFetchers(importFormatPreferences, importerPreferences)
                                    .stream()
                                    .map(SearchBasedFetcher::getName)
                                    // The user wants to select specific fetchers
                                    // The fetcher summarizing ALL fetchers can be emulated by selecting ALL fetchers (which happens rarely when doing an SLR)
                                    .filter(name -> !name.equals(CompositeSearchBasedFetcher.FETCHER_NAME))
                                    .map(name -> {
                                        boolean enabled = studyDatabases.contains(new StudyDatabase(name, true));
                                        return new StudyDatabaseItem(name, enabled);
                                    })
                                    .toList());

        this.directory.set(Objects.requireNonNull(studyDirectory).toString());
        this.dialogService = Objects.requireNonNull(dialogService);
    }

    public StringProperty getTitle() {
        return title;
    }

    public StringProperty getDirectory() {
        return directory;
    }

    public ObservableList<String> getAuthors() {
        return authors;
    }

    public ObservableList<String> getResearchQuestions() {
        return researchQuestions;
    }

    public ObservableList<String> getQueries() {
        return queries;
    }

    public ObservableList<StudyDatabaseItem> getDatabases() {
        return databases;
    }

    public void addAuthor(String author) {
        if (author.isBlank()) {
            return;
        }
        authors.add(author);
    }

    public void addResearchQuestion(String researchQuestion) {
        if (researchQuestion.isBlank() || researchQuestions.contains(researchQuestion)) {
            return;
        }
        researchQuestions.add(researchQuestion);
    }

    public void addQuery(String query) {
        if (query.isBlank()) {
            return;
        }
        queries.add(query);
    }

    public SlrStudyAndDirectory saveStudy() {
        Study study = new Study(
                authors,
                title.getValueSafe(),
                researchQuestions,
                queries.stream().map(StudyQuery::new).collect(Collectors.toList()),
                databases.stream().map(studyDatabaseItem -> new StudyDatabase(studyDatabaseItem.getName(), studyDatabaseItem.isEnabled())).filter(StudyDatabase::isEnabled).collect(Collectors.toList()));
        Path studyDirectory;
        try {
            studyDirectory = Path.of(directory.getValueSafe());
        } catch (InvalidPathException e) {
            LOGGER.error("Invalid path was provided: {}", directory.getValueSafe());
            // This will appear very seldom, thus we accept that we use "file path" instead of "directory"
            dialogService.notify(Localization.lang("Please enter a valid file path.") +
                    ": " + directory.getValueSafe());
            // We do not assume another path - we return that there is an invalid object.
            return null;
        }
        return new SlrStudyAndDirectory(study, studyDirectory);
    }

    public Property<String> titleProperty() {
        return title;
    }

    public void setStudyDirectory(Optional<Path> studyRepositoryRoot) {
        getDirectory().setValue(studyRepositoryRoot.map(Path::toString).orElseGet(() -> getDirectory().getValueSafe()));
    }

    public void deleteAuthor(String item) {
        authors.remove(item);
    }

    public void deleteQuestion(String item) {
        researchQuestions.remove(item);
    }

    public void deleteQuery(String item) {
        queries.remove(item);
    }
}
