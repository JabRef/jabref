package org.jabref.logic.crawler;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test of the components used for SLR support
 */
class CrawlerTest {
    @TempDir
    Path tempRepositoryDirectory;
    SavePreferences preferences;
    BibEntryTypesManager entryTypesManager;

    @Test
    public void testWhetherAllFilesAreCreated() throws Exception {
        setUp();
        Crawler testCrawler = new Crawler(getPathToStudyDefinitionFile(),
                new DummyFileUpdateMonitor(),
                preferences,
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
        preferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(preferences.getEncoding()).thenReturn(null);
        when(preferences.takeMetadataSaveOrderInAccount()).thenReturn(true);
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
