package org.jabref.logic.crawler;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;

import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudyCatalogToFetcherConverterTest {
    SaveConfiguration saveConfiguration;
    CliPreferences preferences;
    BibEntryTypesManager entryTypesManager;
    SlrGitHandler gitHandler;
    @TempDir
    Path tempRepositoryDirectory;

    @BeforeEach
    void setUpMocks() {
        preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        saveConfiguration = mock(SaveConfiguration.class, Answers.RETURNS_DEEP_STUBS);
        when(saveConfiguration.getSaveOrder()).thenReturn(SaveOrder.getDefaultSaveOrder());
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getImporterPreferences().getApiKeys()).thenReturn(FXCollections.emptyObservableSet());

        entryTypesManager = new BibEntryTypesManager();
        gitHandler = mock(SlrGitHandler.class, Answers.RETURNS_DEFAULTS);
    }

    @Test
    void getActiveFetcherInstances() throws Exception {
        Path studyDefinition = tempRepositoryDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        copyTestStudyDefinitionFileIntoDirectory(studyDefinition);

        StudyRepository studyRepository = new StudyRepository(
                tempRepositoryDirectory,
                gitHandler,
                preferences,
                new DummyFileUpdateMonitor(),
                entryTypesManager);
        StudyCatalogToFetcherConverter converter = new StudyCatalogToFetcherConverter(
                studyRepository.getActiveLibraryEntries(),
                mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
                mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS));
        List<SearchBasedFetcher> result = converter.getActiveFetchers();

        assertEquals(
                List.of("Springer", "ArXiv", "Medline/PubMed"),
                result.stream().map(SearchBasedFetcher::getName).collect(Collectors.toList())
        );
    }

    private void copyTestStudyDefinitionFileIntoDirectory(Path destination) throws Exception {
        URL studyDefinition = this.getClass().getResource(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, false);
    }
}
