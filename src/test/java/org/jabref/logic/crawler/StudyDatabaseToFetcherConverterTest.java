package org.jabref.logic.crawler;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;

import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudyDatabaseToFetcherConverterTest {
    GeneralPreferences generalPreferences;
    ImportFormatPreferences importFormatPreferences;
    ImporterPreferences importerPreferences;
    SavePreferences savePreferences;
    TimestampPreferences timestampPreferences;
    BibEntryTypesManager entryTypesManager;
    SlrGitHandler gitHandler;
    @TempDir
    Path tempRepositoryDirectory;

    @BeforeEach
    void setUpMocks() {
        generalPreferences = mock(GeneralPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importerPreferences = mock(ImporterPreferences.class);
        savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        timestampPreferences = mock(TimestampPreferences.class);
        when(savePreferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(savePreferences.takeMetadataSaveOrderInAccount()).thenReturn(true);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        entryTypesManager = new BibEntryTypesManager();
        gitHandler = mock(SlrGitHandler.class, Answers.RETURNS_DEFAULTS);
    }

    @Test
    public void getActiveFetcherInstances() throws Exception {
        Path studyDefinition = tempRepositoryDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        copyTestStudyDefinitionFileIntoDirectory(studyDefinition);

        StudyRepository studyRepository = new StudyRepository(
                tempRepositoryDirectory,
                gitHandler,
                generalPreferences,
                importFormatPreferences,
                importerPreferences,
                new DummyFileUpdateMonitor(),
                savePreferences,
                entryTypesManager);
        StudyDatabaseToFetcherConverter converter = new StudyDatabaseToFetcherConverter(
                studyRepository.getActiveLibraryEntries(),
                importFormatPreferences,
                importerPreferences);
        List<SearchBasedFetcher> result = converter.getActiveFetchers();

        Assertions.assertEquals(
                List.of("Springer", "ArXiv", "Medline/PubMed"),
                result.stream().map(SearchBasedFetcher::getName).collect(Collectors.toList())
        );
    }

    private void copyTestStudyDefinitionFileIntoDirectory(Path destination) throws Exception {
        URL studyDefinition = this.getClass().getResource(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, false);
    }
}
