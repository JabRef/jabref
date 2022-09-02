package org.jabref.gui.slr;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
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

    private final StringProperty title = new SimpleStringProperty();
    private final ObservableList<String> authors = FXCollections.observableArrayList();
    private final ObservableList<String> researchQuestions = FXCollections.observableArrayList();
    private final ObservableList<String> queries = FXCollections.observableArrayList();
    private final ObservableList<StudyDatabaseItem> databases = FXCollections.observableArrayList();
    // Hold the complement of databases for the selector
    private final SimpleStringProperty directory = new SimpleStringProperty();
    private Study study;

    public ManageStudyDefinitionViewModel(Study study,
                                          Path studyDirectory,
                                          ImportFormatPreferences importFormatPreferences,
                                          ImporterPreferences importerPreferences) {
        if (Objects.isNull(study)) {
            computeNonSelectedDatabases(importFormatPreferences, importerPreferences);
            return;
        }
        this.study = study;
        title.setValue(study.getTitle());
        authors.addAll(study.getAuthors());
        researchQuestions.addAll(study.getResearchQuestions());
        queries.addAll(study.getQueries().stream().map(StudyQuery::getQuery).toList());

        // Currently, modifying an existing study is not possible. But this code first adds all databases that are
        // enabled for an existing Study object as enabled entries to the table and afterwards populates the table with
        // all other available databases.
        databases.addAll(study.getDatabases()
                              .stream()
                              .map(studyDatabase -> new StudyDatabaseItem(studyDatabase.getName(), studyDatabase.isEnabled()))
                              .toList());
        computeNonSelectedDatabases(importFormatPreferences, importerPreferences);
        if (!Objects.isNull(studyDirectory)) {
            this.directory.set(studyDirectory.toString());
        }
    }

    private void computeNonSelectedDatabases(ImportFormatPreferences importFormatPreferences, ImporterPreferences importerPreferences) {
        databases.addAll(WebFetchers.getSearchBasedFetchers(importFormatPreferences, importerPreferences)
                                    .stream()
                                    .map(SearchBasedFetcher::getName)
                                    .map(s -> new StudyDatabaseItem(s, false))
                                    .filter(studyDatabase -> !databases.contains(studyDatabase)).toList());
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
        if (Objects.isNull(study)) {
            study = new Study();
        }
        study.setTitle(title.getValueSafe());
        study.setAuthors(authors);
        study.setResearchQuestions(researchQuestions);
        study.setQueries(queries.stream().map(StudyQuery::new).collect(Collectors.toList()));
        study.setDatabases(databases.stream().map(studyDatabaseItem -> new StudyDatabase(studyDatabaseItem.getName(), studyDatabaseItem.isEnabled())).filter(StudyDatabase::isEnabled).collect(Collectors.toList()));
        Path studyDirectory = null;
        try {
            studyDirectory = Path.of(directory.getValueSafe());
        } catch (
                InvalidPathException e) {
            LOGGER.error("Invalid path was provided: {}", directory);
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
