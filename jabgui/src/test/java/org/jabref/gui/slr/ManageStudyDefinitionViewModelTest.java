package org.jabref.gui.slr;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.gui.DialogService;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.study.Study;
import org.jabref.model.study.StudyCatalog;
import org.jabref.model.study.StudyQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ManageStudyDefinitionViewModelTest {
    private ImportFormatPreferences importFormatPreferences;
    private ImporterPreferences importerPreferences;
    private WorkspacePreferences workspacePreferences;
    private GitPreferences gitPreferences;

    private DialogService dialogService;

    @BeforeEach
    void setUp() {
        // code taken from org.jabref.logic.importer.WebFetchersTest.setUp
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        gitPreferences = mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS);
        dialogService = mock(DialogService.class);
    }

    @Test
    void emptyStudyConstructorFillsDatabasesCorrectly() {
        ManageStudyDefinitionViewModel manageStudyDefinitionViewModel = new ManageStudyDefinitionViewModel(
                importFormatPreferences, importerPreferences, workspacePreferences, gitPreferences, dialogService);
        assertEquals(List.of(
                new StudyCatalogItem("ACM Portal", true),
                new StudyCatalogItem("arXiv", false),
                new StudyCatalogItem("Bibliotheksverbund Bayern (Experimental)", false),
                new StudyCatalogItem("Bielefeld Academic Search Engine", false),
                new StudyCatalogItem("Biodiversity Heritage", false),
                new StudyCatalogItem("Crossref", false),
                new StudyCatalogItem("DBLP", true),
                new StudyCatalogItem("DNB", false),
                new StudyCatalogItem("DOAB", false),
                new StudyCatalogItem("DOAJ", false),
                new StudyCatalogItem("Europe/PMCID", false),
                new StudyCatalogItem("GVK", false),
                new StudyCatalogItem("IEEEXplore", true),
                new StudyCatalogItem("INSPIRE", false),
                new StudyCatalogItem("ISIDORE", false),
                new StudyCatalogItem("LOBID", false),
                new StudyCatalogItem("MathSciNet", false),
                new StudyCatalogItem("Medline/PubMed", false),
                new StudyCatalogItem("OpenAlex", false),
                new StudyCatalogItem("ResearchGate", false),
                new StudyCatalogItem("SAO/NASA ADS", false),
                new StudyCatalogItem("ScholarAPI", false),
                new StudyCatalogItem("ScholarArchive", false),
                new StudyCatalogItem("Scopus", false),
                new StudyCatalogItem("SemanticScholar", false),
                new StudyCatalogItem("Springer", true),
                new StudyCatalogItem("Unpaywall", false),
                new StudyCatalogItem("zbMATH", false)
        ), manageStudyDefinitionViewModel.getCatalogs());
    }

    @Test
    void studyConstructorFillsDatabasesCorrectly(@TempDir Path tempDir) {
        ManageStudyDefinitionViewModel manageStudyDefinitionViewModel = getManageStudyDefinitionViewModel(tempDir);
        assertEquals(List.of(
                new StudyCatalogItem("ACM Portal", true),
                new StudyCatalogItem("arXiv", false),
                new StudyCatalogItem("Bibliotheksverbund Bayern (Experimental)", false),
                new StudyCatalogItem("Bielefeld Academic Search Engine", false),
                new StudyCatalogItem("Biodiversity Heritage", false),
                new StudyCatalogItem("Crossref", false),
                new StudyCatalogItem("DBLP", false),
                new StudyCatalogItem("DNB", false),
                new StudyCatalogItem("DOAB", false),
                new StudyCatalogItem("DOAJ", false),
                new StudyCatalogItem("Europe/PMCID", false),
                new StudyCatalogItem("GVK", false),
                new StudyCatalogItem("IEEEXplore", false),
                new StudyCatalogItem("INSPIRE", false),
                new StudyCatalogItem("ISIDORE", false),
                new StudyCatalogItem("LOBID", false),
                new StudyCatalogItem("MathSciNet", false),
                new StudyCatalogItem("Medline/PubMed", false),
                new StudyCatalogItem("OpenAlex", false),
                new StudyCatalogItem("ResearchGate", false),
                new StudyCatalogItem("SAO/NASA ADS", false),
                new StudyCatalogItem("ScholarAPI", false),
                new StudyCatalogItem("ScholarArchive", false),
                new StudyCatalogItem("Scopus", false),
                new StudyCatalogItem("SemanticScholar", false),
                new StudyCatalogItem("Springer", false),
                new StudyCatalogItem("Unpaywall", false),
                new StudyCatalogItem("zbMATH", false)
        ), manageStudyDefinitionViewModel.getCatalogs());
    }

    @Test
    void studyConstructorPreservesCatalogReason(@TempDir Path tempDir) {
        List<StudyCatalog> catalogs = List.of(
                new StudyCatalog("ACM Portal", true, "Primary source"));
        Study study = new Study(
                List.of("Name"),
                "title",
                List.of("Q1"),
                List.of(),
                catalogs
        );
        ManageStudyDefinitionViewModel viewModel = new ManageStudyDefinitionViewModel(
                study,
                tempDir,
                importFormatPreferences,
                importerPreferences,
                workspacePreferences,
                gitPreferences,
                dialogService);
        String reason = viewModel.getCatalogs().stream()
                                 .filter(item -> "ACM Portal".equals(item.getName()))
                                 .findFirst()
                                 .map(StudyCatalogItem::getReason)
                                 .orElse("");
        assertEquals("Primary source", reason);
    }

    @Test
    void saveStudyHasCurrentSchemaVersion(@TempDir Path tempDir) {
        ManageStudyDefinitionViewModel viewModel = getManageStudyDefinitionViewModel(tempDir);
        SlrStudyAndDirectory result = viewModel.saveStudy();
        assertEquals(Study.CURRENT_SCHEMA_VERSION, result.getStudy().getVersion());
    }

    @Test
    void nativeQueryPropagatesToEveryQueriesCatalogSpecificMap(@TempDir Path tempDir) {
        StudyQuery query1 = new StudyQuery("Q1");
        StudyQuery query2 = new StudyQuery("Q2");
        List<StudyCatalog> catalogs = List.of(new StudyCatalog("ACM Portal", true));
        Study study = new Study(List.of("Name"), "title", List.of("RQ1"), List.of(query1, query2), catalogs);
        ManageStudyDefinitionViewModel viewModel = new ManageStudyDefinitionViewModel(
                study, tempDir, importFormatPreferences, importerPreferences, workspacePreferences, gitPreferences, dialogService);

        viewModel.getCatalogs().stream()
                 .filter(item -> "ACM Portal".equals(item.getName()))
                 .findFirst()
                 .orElseThrow()
                 .setNativeQuery("ti:Test");

        Study builtStudy = viewModel.buildStudy();
        for (StudyQuery query : builtStudy.getQueries()) {
            assertEquals(Map.of("ACM Portal", "ti:Test"), query.getCatalogSpecific());
        }
    }

    @Test
    void blankNativeQueryRemovesCatalogSpecificKeyInsteadOfWritingEmptyString(@TempDir Path tempDir) {
        StudyQuery query = new StudyQuery("Q1");
        query.getCatalogSpecific().put("ACM Portal", "ti:Existing");
        List<StudyCatalog> catalogs = List.of(new StudyCatalog("ACM Portal", true));
        Study study = new Study(List.of("Name"), "title", List.of("RQ1"), List.of(query), catalogs);
        ManageStudyDefinitionViewModel viewModel = new ManageStudyDefinitionViewModel(
                study, tempDir, importFormatPreferences, importerPreferences, workspacePreferences, gitPreferences, dialogService);

        viewModel.getCatalogs().stream()
                 .filter(item -> "ACM Portal".equals(item.getName()))
                 .findFirst()
                 .orElseThrow()
                 .setNativeQuery("");

        Study builtStudy = viewModel.buildStudy();
        assertEquals(Map.of(), builtStudy.getQueries().getFirst().getCatalogSpecific());
    }

    @Test
    void disabledCatalogNativeQueryIsNotApplied(@TempDir Path tempDir) {
        StudyQuery query = new StudyQuery("Q1");
        Study study = new Study(List.of("Name"), "title", List.of("RQ1"), List.of(query), List.of());
        ManageStudyDefinitionViewModel viewModel = new ManageStudyDefinitionViewModel(
                study, tempDir, importFormatPreferences, importerPreferences, workspacePreferences, gitPreferences, dialogService);

        // "arXiv" is not part of the study's catalogs, so it stays disabled by default
        viewModel.getCatalogs().stream()
                 .filter(item -> "arXiv".equals(item.getName()))
                 .findFirst()
                 .orElseThrow()
                 .setNativeQuery("ti:Test");

        Study builtStudy = viewModel.buildStudy();
        assertEquals(Map.of(), builtStudy.getQueries().getFirst().getCatalogSpecific());
    }

    @Test
    void applyNativeQueryOverridesMatchesCatalogNameCaseInsensitively(@TempDir Path tempDir) {
        StudyQuery query = new StudyQuery("Q1");
        query.getCatalogSpecific().put("acm portal", "ti:Old");
        List<StudyCatalog> catalogs = List.of(new StudyCatalog("ACM Portal", true));
        Study study = new Study(List.of("Name"), "title", List.of("RQ1"), List.of(query), catalogs);
        ManageStudyDefinitionViewModel viewModel = new ManageStudyDefinitionViewModel(
                study, tempDir, importFormatPreferences, importerPreferences, workspacePreferences, gitPreferences, dialogService);

        viewModel.getCatalogs().stream()
                 .filter(item -> "ACM Portal".equals(item.getName()))
                 .findFirst()
                 .orElseThrow()
                 .setNativeQuery("ti:New");

        Study builtStudy = viewModel.buildStudy();
        // The differently-cased key must be replaced, not kept alongside the new one
        assertEquals(Map.of("ACM Portal", "ti:New"), builtStudy.getQueries().getFirst().getCatalogSpecific());
    }

    @Test
    void existingStudyLoadPicksFirstNonBlankCatalogSpecificOverrideInQueryOrder(@TempDir Path tempDir) {
        StudyQuery query1 = new StudyQuery("Q1");
        query1.getCatalogSpecific().put("ACM Portal", " ");
        StudyQuery query2 = new StudyQuery("Q2");
        query2.getCatalogSpecific().put("ACM Portal", "ti:First");
        StudyQuery query3 = new StudyQuery("Q3");
        query3.getCatalogSpecific().put("ACM Portal", "ti:Second");
        List<StudyCatalog> catalogs = List.of(new StudyCatalog("ACM Portal", true));
        Study study = new Study(List.of("Name"), "title", List.of("RQ1"), List.of(query1, query2, query3), catalogs);

        ManageStudyDefinitionViewModel viewModel = new ManageStudyDefinitionViewModel(
                study, tempDir, importFormatPreferences, importerPreferences, workspacePreferences, gitPreferences, dialogService);

        String nativeQuery = viewModel.getCatalogs().stream()
                                      .filter(item -> "ACM Portal".equals(item.getName()))
                                      .findFirst()
                                      .map(StudyCatalogItem::getNativeQuery)
                                      .orElse("");

        assertEquals("ti:First", nativeQuery);
    }

    private ManageStudyDefinitionViewModel getManageStudyDefinitionViewModel(Path tempDir) {
        List<StudyCatalog> catalogs = List.of(
                new StudyCatalog("ACM Portal", true));
        Study study = new Study(
                List.of("Name"),
                "title",
                List.of("Q1"),
                List.of(),
                catalogs
        );
        return new ManageStudyDefinitionViewModel(
                study,
                tempDir,
                importFormatPreferences,
                importerPreferences,
                workspacePreferences,
                gitPreferences,
                dialogService);
    }
}
