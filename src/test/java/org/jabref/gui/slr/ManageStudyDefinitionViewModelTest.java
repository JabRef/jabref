package org.jabref.gui.slr;

import java.util.List;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManageStudyDefinitionViewModelTest {
    private ImportFormatPreferences importFormatPreferences;
    private ImporterPreferences importerPreferences;

    @BeforeEach
    void setUp() {
        // code taken from org.jabref.logic.importer.WebFetchersTest.setUp
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importerPreferences = mock(ImporterPreferences.class);
        FieldContentFormatterPreferences fieldContentFormatterPreferences = mock(FieldContentFormatterPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(fieldContentFormatterPreferences);
    }

    @Test
    public void constructorFillsDatabasesCorrectly() {
        ManageStudyDefinitionViewModel manageStudyDefinitionViewModel = new ManageStudyDefinitionViewModel(importFormatPreferences, importerPreferences);
        assertEquals(List.of(
            new StudyDatabaseItem("ACM Portal", true),
            new StudyDatabaseItem("ArXiv", false),
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
}
