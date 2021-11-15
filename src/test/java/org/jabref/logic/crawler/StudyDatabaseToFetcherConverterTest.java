package org.jabref.logic.crawler;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
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
        savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        timestampPreferences = mock(TimestampPreferences.class);
        when(generalPreferences.getDefaultEncoding()).thenReturn(Charset.defaultCharset());
        when(savePreferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(savePreferences.takeMetadataSaveOrderInAccount()).thenReturn(true);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(new FieldContentFormatterPreferences());
        when(importFormatPreferences.isKeywordSyncEnabled()).thenReturn(false);
        entryTypesManager = new BibEntryTypesManager();
        gitHandler = mock(SlrGitHandler.class, Answers.RETURNS_DEFAULTS);
    }

    @Test
    public void getActiveFetcherInstances() throws Exception {
        Path studyDefinition = tempRepositoryDirectory.resolve("study.yml");
        copyTestStudyDefinitionFileIntoDirectory(studyDefinition);

        StudyRepository studyRepository = new StudyRepository(tempRepositoryDirectory, gitHandler, generalPreferences, importFormatPreferences, new DummyFileUpdateMonitor(), savePreferences, entryTypesManager);
        StudyDatabaseToFetcherConverter converter = new StudyDatabaseToFetcherConverter(studyRepository.getActiveLibraryEntries(), importFormatPreferences);
        List<SearchBasedFetcher> result = converter.getActiveFetchers();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(result.get(0).getName(), "Springer");
        Assertions.assertEquals(result.get(1).getName(), "ArXiv");
    }

    private void copyTestStudyDefinitionFileIntoDirectory(Path destination) throws Exception {
        URL studyDefinition = this.getClass().getResource("study.yml");
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, false);
    }
}
