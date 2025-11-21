package org.jabref.logic.crawler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.JabRefException;
import org.jabref.logic.citationkeypattern.CitationKeyGeneratorTestUtils;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.FetcherTest;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test of the components used for SLR support
 * Marked as FetcherTest as it calls fetcher
 */
@FetcherTest
class CrawlerTest {
    @TempDir
    Path tempRepositoryDirectory;
    ImportFormatPreferences importFormatPreferences;
    ImporterPreferences importerPreferences;
    SaveConfiguration saveConfiguration;
    BibEntryTypesManager entryTypesManager;
    SlrGitHandler gitHandler = mock(SlrGitHandler.class, Answers.RETURNS_DEFAULTS);
    String hashCodeQuantum = String.valueOf("Quantum".hashCode());
    String hashCodeCloudComputing = String.valueOf("Cloud Computing".hashCode());

    CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);

    /**
     * Set up mocks and copies the study definition file into the test repository
     */
    @BeforeEach
    void setUp() throws GitAPIException, URISyntaxException {
        setUpRepository();

        CitationKeyPatternPreferences citationKeyPatternPreferences = CitationKeyGeneratorTestUtils.getInstanceForTesting();

        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importerPreferences = mock(ImporterPreferences.class);
        saveConfiguration = mock(SaveConfiguration.class, Answers.RETURNS_DEEP_STUBS);
        when(saveConfiguration.getSaveOrder()).thenReturn(SaveOrder.getDefaultSaveOrder());
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        when(preferences.getCitationKeyPatternPreferences()).thenReturn(citationKeyPatternPreferences);

        entryTypesManager = new BibEntryTypesManager();
    }

    private void setUpRepository() throws GitAPIException, URISyntaxException {
        Git git = Git.init()
                     .setDirectory(tempRepositoryDirectory.toFile())
                     .call();
        setUpTestStudyDefinitionFile();
        git.add()
           .addFilepattern(".")
           .call();
        git.commit()
           .setMessage("Initialize")
           .call();
        git.close();
    }

    private void setUpTestStudyDefinitionFile() throws URISyntaxException {
        Path destination = tempRepositoryDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        URL studyDefinition = this.getClass().getResource(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, false);
    }

    @Test
    void whetherAllFilesAreCreated() throws IOException, ParseException, GitAPIException, SaveException, JabRefException {
        Crawler testCrawler = new Crawler(getPathToStudyDefinitionFile(),
                gitHandler,
                preferences,
                entryTypesManager,
                new DummyFileUpdateMonitor(),
                new DummyDirectoryUpdateMonitor());

        testCrawler.performCrawl();

        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeQuantum + " - Quantum")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeCloudComputing + " - Cloud Computing")));

        List<String> filesToAssert = List.of("ArXiv.bib", "Springer.bib", "result.bib", "Medline_PubMed.bib");
        filesToAssert.forEach(
                fileName -> {
                    assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeQuantum + " - Quantum", fileName)));
                    assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeCloudComputing + " - Cloud Computing", fileName)));
                });
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "studyResult.bib")));
    }

    private Path getPathToStudyDefinitionFile() {
        return tempRepositoryDirectory;
    }
}
