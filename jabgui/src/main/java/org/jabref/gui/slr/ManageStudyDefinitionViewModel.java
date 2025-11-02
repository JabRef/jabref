package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.logic.crawler.StudyRepository;
import org.jabref.logic.crawler.StudyYamlParser;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.ACMPortalFetcher;
import org.jabref.logic.importer.fetcher.CompositeSearchBasedFetcher;
import org.jabref.logic.importer.fetcher.DBLPFetcher;
import org.jabref.logic.importer.fetcher.IEEE;
import org.jabref.logic.importer.fetcher.SpringerNatureWebFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;
import org.jabref.model.study.StudyQuery;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.NonNull;
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
            SpringerNatureWebFetcher.FETCHER_NAME,
            DBLPFetcher.FETCHER_NAME);

    private final StringProperty title = new SimpleStringProperty();
    private final ObservableList<String> authors = FXCollections.observableArrayList();
    private final ObservableList<String> researchQuestions = FXCollections.observableArrayList();
    private final ObservableList<String> queries = FXCollections.observableArrayList();
    private final ObservableList<StudyCatalogItem> databases = FXCollections.observableArrayList();

    // Hold the complement of databases for the selector
    private final SimpleStringProperty directory = new SimpleStringProperty();

    private final DialogService dialogService;

    private final WorkspacePreferences workspacePreferences;

    private final StringProperty titleValidationMessage = new SimpleStringProperty();
    private final StringProperty authorsValidationMessage = new SimpleStringProperty();
    private final StringProperty questionsValidationMessage = new SimpleStringProperty();
    private final StringProperty queriesValidationMessage = new SimpleStringProperty();
    private final StringProperty catalogsValidationMessage = new SimpleStringProperty();
    private final StringProperty validationHeaderMessage = new SimpleStringProperty();

    /**
     * Constructor for a new study
     */
    public ManageStudyDefinitionViewModel(ImportFormatPreferences importFormatPreferences,
                                          ImporterPreferences importerPreferences,
                                          @NonNull WorkspacePreferences workspacePreferences,
                                          @NonNull DialogService dialogService) {
        databases.addAll(WebFetchers.getSearchBasedFetchers(importFormatPreferences, importerPreferences)
                                    .stream()
                                    .map(SearchBasedFetcher::getName)
                                    // The user wants to select specific fetchers
                                    // The fetcher summarizing ALL fetchers can be emulated by selecting ALL fetchers (which happens rarely when doing an SLR)
                                    .filter(name -> !CompositeSearchBasedFetcher.FETCHER_NAME.equals(name))
                                    .map(name -> {
                                        boolean enabled = DEFAULT_SELECTION.contains(name);
                                        return new StudyCatalogItem(name, enabled);
                                    })
                                    .toList());
        this.dialogService = dialogService;
        this.workspacePreferences = workspacePreferences;

        initializeValidationBindings();
    }

    /**
     * Constructor for an existing study
     *
     * @param study          The study to initialize the UI from
     * @param studyDirectory The path where the study resides
     */
    public ManageStudyDefinitionViewModel(@NonNull Study study,
                                          @NonNull Path studyDirectory,
                                          ImportFormatPreferences importFormatPreferences,
                                          ImporterPreferences importerPreferences,
                                          @NonNull WorkspacePreferences workspacePreferences,
                                          @NonNull DialogService dialogService) {
        // copy the content of the study object into the UI fields
        authors.addAll(study.getAuthors());
        title.setValue(study.getTitle());
        researchQuestions.addAll(study.getResearchQuestions());
        queries.addAll(study.getQueries().stream().map(StudyQuery::getQuery).toList());
        List<StudyDatabase> studyDatabases = study.getDatabases();
        databases.addAll(WebFetchers.getSearchBasedFetchers(importFormatPreferences, importerPreferences)
                                    .stream()
                                    .map(SearchBasedFetcher::getName)
                                    // The user wants to select specific fetchers
                                    // The fetcher summarizing ALL fetchers can be emulated by selecting ALL fetchers (which happens rarely when doing an SLR)
                                    .filter(name -> !CompositeSearchBasedFetcher.FETCHER_NAME.equals(name))
                                    .map(name -> {
                                        boolean enabled = studyDatabases.contains(new StudyDatabase(name, true));
                                        return new StudyCatalogItem(name, enabled);
                                    })
                                    .toList());

        this.directory.set(studyDirectory.toString());
        this.workspacePreferences = workspacePreferences;
        this.dialogService = dialogService;

        initializeValidationBindings();
    }

    private void initializeValidationBindings() {
        titleValidationMessage.bind(Bindings.when(title.isEmpty())
                .then(Localization.lang("Study title is required"))
                .otherwise(""));

        authorsValidationMessage.bind(Bindings.when(Bindings.isEmpty(authors))
                .then(Localization.lang("At least one author is required"))
                .otherwise(""));

        questionsValidationMessage.bind(Bindings.when(Bindings.isEmpty(researchQuestions))
                .then(Localization.lang("At least one research question is required"))
                .otherwise(""));

        queriesValidationMessage.bind(Bindings.when(Bindings.isEmpty(queries))
                .then(Localization.lang("At least one query is required"))
                .otherwise(""));

        catalogsValidationMessage.bind(Bindings.when(
                Bindings.createBooleanBinding(() ->
                    databases.stream().noneMatch(StudyCatalogItem::isEnabled), databases))
                .then(Localization.lang("At least one catalog must be selected"))
                .otherwise(""));

        validationHeaderMessage.bind(Bindings.when(
                Bindings.or(
                    Bindings.or(
                        Bindings.or(
                            Bindings.or(title.isEmpty(), Bindings.isEmpty(authors)),
                            Bindings.isEmpty(researchQuestions)
                        ),
                        Bindings.isEmpty(queries)
                    ),
                    Bindings.createBooleanBinding(() ->
                        databases.stream().noneMatch(StudyCatalogItem::isEnabled), databases)
                ))
                .then(Localization.lang("In order to proceed:"))
                .otherwise(""));
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

    public ObservableList<StudyCatalogItem> getCatalogs() {
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
        final String studyDirectoryAsString = directory.getValueSafe();
        try {
            studyDirectory = Path.of(studyDirectoryAsString);
        } catch (InvalidPathException e) {
            LOGGER.error("Invalid path was provided: {}", studyDirectoryAsString);
            dialogService.notify(Localization.lang("Unable to write to %0.", studyDirectoryAsString));
            // We do not assume another path - we return that there is an invalid object.
            return null;
        }
        Path studyDefinitionFile = studyDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        try {
            new StudyYamlParser().writeStudyYamlFile(study, studyDefinitionFile);
        } catch (IOException e) {
            LOGGER.error("Could not write study file {}", studyDefinitionFile, e);
            dialogService.notify(Localization.lang("Please enter a valid file path.") +
                    ": " + studyDirectoryAsString);
            // We do not assume another path - we return that there is an invalid object.
            return null;
        }

        try {
            new GitHandler(studyDirectory).createCommitOnCurrentBranch("Update study definition", false);
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Could not commit study definition file in directory {}", studyDirectory, e);
            dialogService.notify(Localization.lang("Please enter a valid file path.") +
                    ": " + studyDirectory);
            // We continue nevertheless as the directory itself could be valid
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

    public void initializeSelectedCatalogs() {
        List<String> selectedCatalogs = workspacePreferences.getSelectedSlrCatalogs();
        for (StudyCatalogItem catalog : databases) {
            catalog.setEnabled(selectedCatalogs.contains(catalog.getName()));
        }
    }

    public void updateSelectedCatalogs() {
        List<String> selectedCatalogsList = databases.stream()
                                                     .filter(StudyCatalogItem::isEnabled)
                                                     .map(StudyCatalogItem::getName)
                                                     .collect(Collectors.toList());

        workspacePreferences.setSelectedSlrCatalogs(selectedCatalogsList);
    }

    public StringProperty validationHeaderMessageProperty() {
        return validationHeaderMessage;
    }

    public StringProperty titleValidationMessageProperty() {
        return titleValidationMessage;
    }

    public StringProperty authorsValidationMessageProperty() {
        return authorsValidationMessage;
    }

    public StringProperty questionsValidationMessageProperty() {
        return questionsValidationMessage;
    }

    public StringProperty queriesValidationMessageProperty() {
        return queriesValidationMessage;
    }

    public StringProperty catalogsValidationMessageProperty() {
        return catalogsValidationMessage;
    }
}
