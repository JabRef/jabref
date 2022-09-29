package org.jabref.logic.crawler;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.jabref.logic.citationkeypattern.CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test of the components used for SLR support
 */
class CrawlerTest {
    @TempDir
    Path tempRepositoryDirectory;
    ImportFormatPreferences importFormatPreferences;
    ImporterPreferences importerPreferences;
    SavePreferences savePreferences;
    BibEntryTypesManager entryTypesManager;
    SlrGitHandler gitHandler = mock(SlrGitHandler.class, Answers.RETURNS_DEFAULTS);
    String hashCodeQuantum = String.valueOf("Quantum".hashCode());
    String hashCodeCloudComputing = String.valueOf("Cloud Computing".hashCode());

    GeneralPreferences generalPreferences = mock(GeneralPreferences.class, Answers.RETURNS_DEEP_STUBS);

    @Test
    public void testWhetherAllFilesAreCreated() throws Exception {
        setUp();
        Crawler testCrawler = new Crawler(getPathToStudyDefinitionFile(),
                gitHandler,
                generalPreferences,
                importFormatPreferences,
                importerPreferences,
                savePreferences,
                entryTypesManager,
                new DummyFileUpdateMonitor());

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

    /**
     * Set up mocks and copies the study definition file into the test repository
     */
    private void setUp() throws Exception {
        setUpRepository();

        CitationKeyPatternPreferences citationKeyPatternPreferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                GlobalCitationKeyPattern.fromPattern("[auth][year]"),
                ',');

        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importerPreferences = mock(ImporterPreferences.class);
        savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(savePreferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(savePreferences.takeMetadataSaveOrderInAccount()).thenReturn(true);
        when(savePreferences.getCitationKeyPatternPreferences()).thenReturn(citationKeyPatternPreferences);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(new FieldContentFormatterPreferences());
        entryTypesManager = new BibEntryTypesManager();
    }

    private void setUpRepository() throws Exception {
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

    private void setUpTestStudyDefinitionFile() throws Exception {
        Path destination = tempRepositoryDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        URL studyDefinition = this.getClass().getResource(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, false);
    }
}
