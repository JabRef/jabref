package org.jabref.gui.slr;

import java.nio.file.Path;
import java.util.List;

import org.jabref.gui.DialogService;
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

class ManageStudyDefinitionViewModelTest {
    private ImportFormatPreferences importFormatPreferences;
    private ImporterPreferences importerPreferences;
    private DialogService dialogService;

    @BeforeEach
    void setUp() {
        // code taken from org.jabref.logic.importer.WebFetchersTest.setUp
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        dialogService = mock(DialogService.class);
    }

    @Test
    public void emptyStudyConstructorFillsDatabasesCorrectly() {
        ManageStudyDefinitionViewModel manageStudyDefinitionViewModel = new ManageStudyDefinitionViewModel(importFormatPreferences, importerPreferences, dialogService);
        assertEquals(List.of(
                new StudyCatalogItem("ACM Portal", true),
                new StudyCatalogItem("ArXiv", false),
                new StudyCatalogItem("Bibliotheksverbund Bayern (Experimental)", false),
                new StudyCatalogItem("Biodiversity Heritage", false),
                new StudyCatalogItem("CiteSeerX", false),
                new StudyCatalogItem("Crossref", false),
                new StudyCatalogItem("DBLP", true),
                new StudyCatalogItem("DOAB", false),
                new StudyCatalogItem("DOAJ", false),
                new StudyCatalogItem("GVK", false),
                new StudyCatalogItem("IEEEXplore", true),
                new StudyCatalogItem("INSPIRE", false),
                new StudyCatalogItem("LOBID", false),
                new StudyCatalogItem("MathSciNet", false),
                new StudyCatalogItem("Medline/PubMed", false),
                new StudyCatalogItem("ResearchGate", false),
                new StudyCatalogItem("SAO/NASA ADS", false),
                new StudyCatalogItem("ScholarArchive", false),
                new StudyCatalogItem("SemanticScholar", false),
                new StudyCatalogItem("Springer", true),
                new StudyCatalogItem("zbMATH", false)
        ), manageStudyDefinitionViewModel.getCatalogs());
    }

    @Test
    public void studyConstructorFillsDatabasesCorrectly(@TempDir Path tempDir) {
        ManageStudyDefinitionViewModel manageStudyDefinitionViewModel = getManageStudyDefinitionViewModel(tempDir);
        assertEquals(List.of(
                new StudyCatalogItem("ACM Portal", true),
                new StudyCatalogItem("ArXiv", false),
                new StudyCatalogItem("Bibliotheksverbund Bayern (Experimental)", false),
                new StudyCatalogItem("Biodiversity Heritage", false),
                new StudyCatalogItem("CiteSeerX", false),
                new StudyCatalogItem("Crossref", false),
                new StudyCatalogItem("DBLP", false),
                new StudyCatalogItem("DOAB", false),
                new StudyCatalogItem("DOAJ", false),
                new StudyCatalogItem("GVK", false),
                new StudyCatalogItem("IEEEXplore", false),
                new StudyCatalogItem("INSPIRE", false),
                new StudyCatalogItem("LOBID", false),
                new StudyCatalogItem("MathSciNet", false),
                new StudyCatalogItem("Medline/PubMed", false),
                new StudyCatalogItem("ResearchGate", false),
                new StudyCatalogItem("SAO/NASA ADS", false),
                new StudyCatalogItem("ScholarArchive", false),
                new StudyCatalogItem("SemanticScholar", false),
                new StudyCatalogItem("Springer", false),
                new StudyCatalogItem("zbMATH", false)
        ), manageStudyDefinitionViewModel.getCatalogs());
    }

    private ManageStudyDefinitionViewModel getManageStudyDefinitionViewModel(Path tempDir) {
        List<StudyDatabase> databases = List.of(
                new StudyDatabase("ACM Portal", true));
        Study study = new Study(
                List.of("Name"),
                "title",
                List.of("Q1"),
                List.of(),
                databases
        );
        return new ManageStudyDefinitionViewModel(
                study,
                tempDir,
                importFormatPreferences,
                importerPreferences,
                dialogService);
    }
}
