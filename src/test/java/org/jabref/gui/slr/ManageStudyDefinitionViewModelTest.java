package org.jabref.gui.slr;

import java.nio.file.Path;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManageStudyDefinitionViewModelTest {
    private ImportFormatPreferences importFormatPreferences;
    private ImporterPreferences importerPreferences;
    private DialogService dialogService;

    @BeforeEach
    void setUp() {
        // code taken from org.jabref.logic.importer.WebFetchersTest.setUp
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importerPreferences = mock(ImporterPreferences.class);
        FieldContentFormatterPreferences fieldContentFormatterPreferences = mock(FieldContentFormatterPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(fieldContentFormatterPreferences);
        dialogService = mock(DialogService.class);
    }

    @Test
    public void emptyStudyConstructorFillsDatabasesCorrectly() {
        ManageStudyDefinitionViewModel manageStudyDefinitionViewModel = new ManageStudyDefinitionViewModel(importFormatPreferences, importerPreferences, dialogService);
        assertEquals(List.of(
                new StudyDatabaseItem("ACM Portal", true),
                new StudyDatabaseItem("ArXivFetcher", false),
                new StudyDatabaseItem("Biodiversity Heritage", false),
                new StudyDatabaseItem("CiteSeerX", false),
                new StudyDatabaseItem("Collection of Computer Science Bibliographies", false),
                new StudyDatabaseItem("Crossref", false),
                new StudyDatabaseItem("DBLP", true),
                new StudyDatabaseItem("DOAB", false),
                new StudyDatabaseItem("DOAJ", false),
                new StudyDatabaseItem("GVK", false),
                new StudyDatabaseItem("IEEEXplore", true),
                new StudyDatabaseItem("INSPIRE", false),
                new StudyDatabaseItem("MathSciNet", false),
                new StudyDatabaseItem("Medline/PubMed", false),
                new StudyDatabaseItem("ResearchGate", false),
                new StudyDatabaseItem("SAO/NASA ADS", false),
                new StudyDatabaseItem("SemanticScholar", false),
                new StudyDatabaseItem("Springer", true),
                new StudyDatabaseItem("zbMATH", false)
        ), manageStudyDefinitionViewModel.getDatabases());
    }

    @Test
    public void studyConstructorFillsDatabasesCorrectly(@TempDir Path tempDir) {
        List<StudyDatabase> databases = List.of(
                new StudyDatabase("ACM Portal", true));
        Study study = new Study(
                List.of("Name"),
                "title",
                List.of("Q1"),
                List.of(),
                databases
        );
        ManageStudyDefinitionViewModel manageStudyDefinitionViewModel = new ManageStudyDefinitionViewModel(
                study,
                tempDir,
                importFormatPreferences,
                importerPreferences,
                dialogService);
        assertEquals(List.of(
                new StudyDatabaseItem("ACM Portal", true),
                new StudyDatabaseItem("ArXivFetcher", false),
                new StudyDatabaseItem("Biodiversity Heritage", false),
                new StudyDatabaseItem("CiteSeerX", false),
                new StudyDatabaseItem("Collection of Computer Science Bibliographies", false),
                new StudyDatabaseItem("Crossref", false),
                new StudyDatabaseItem("DBLP", false),
                new StudyDatabaseItem("DOAB", false),
                new StudyDatabaseItem("DOAJ", false),
                new StudyDatabaseItem("GVK", false),
                new StudyDatabaseItem("IEEEXplore", false),
                new StudyDatabaseItem("INSPIRE", false),
                new StudyDatabaseItem("MathSciNet", false),
                new StudyDatabaseItem("Medline/PubMed", false),
                new StudyDatabaseItem("ResearchGate", false),
                new StudyDatabaseItem("SAO/NASA ADS", false),
                new StudyDatabaseItem("SemanticScholar", false),
                new StudyDatabaseItem("Springer", false),
                new StudyDatabaseItem("zbMATH", false)
        ), manageStudyDefinitionViewModel.getDatabases());
    }
}
