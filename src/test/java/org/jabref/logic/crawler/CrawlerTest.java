package org.jabref.logic.crawler;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.crawler.git.GitHandler;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.util.DummyFileUpdateMonitor;

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
    SavePreferences savePreferences;
    TimestampPreferences timestampPreferences;
    BibEntryTypesManager entryTypesManager;
    GitHandler gitHandler = mock(GitHandler.class, Answers.RETURNS_DEFAULTS);

    @Test
    public void testWhetherAllFilesAreCreated() throws Exception {
        setUp();
        Crawler testCrawler = new Crawler(getPathToStudyDefinitionFile(),
                gitHandler,
                new DummyFileUpdateMonitor(),
                importFormatPreferences,
                savePreferences,
                timestampPreferences,
                entryTypesManager
        );

        testCrawler.performCrawl();

        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "1 - Quantum")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "2 - Cloud Computing")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "3 - TestSearchQuery3")));

        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "1 - Quantum", "ArXiv.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "2 - Cloud Computing", "ArXiv.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "3 - TestSearchQuery3", "ArXiv.bib")));

        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "1 - Quantum", "Springer.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "2 - Cloud Computing", "Springer.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "3 - TestSearchQuery3", "Springer.bib")));

        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "1 - Quantum", "result.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "2 - Cloud Computing", "result.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "3 - TestSearchQuery3", "result.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), "studyResult.bib")));
    }

    private Path getPathToStudyDefinitionFile() {
        return tempRepositoryDirectory.resolve("study.bib");
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
        savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        timestampPreferences = mock(TimestampPreferences.class);
        when(savePreferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(savePreferences.getEncoding()).thenReturn(null);
        when(savePreferences.takeMetadataSaveOrderInAccount()).thenReturn(true);
        when(savePreferences.getCitationKeyPatternPreferences()).thenReturn(citationKeyPatternPreferences);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(new FieldContentFormatterPreferences());
        when(importFormatPreferences.isKeywordSyncEnabled()).thenReturn(false);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
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
        Path destination = tempRepositoryDirectory.resolve("study.bib");
        URL studyDefinition = this.getClass().getResource("study.bib");
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, false);
    }
}
