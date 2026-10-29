package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.ExternalServicesTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// [utest->feat~fetchers.bibsonomy~1]
class BibSonomyFetcherTest {

    private final BuildInfo buildInfo = new BuildInfo();

    private BibSonomyFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importerPreferences.getApiKey(BibSonomyFetcher.FETCHER_NAME)).thenReturn(Optional.of(buildInfo.bibSonomyUsername + ":" + buildInfo.bibSonomyApiKey));
        fetcher = new BibSonomyFetcher(importFormatPreferences, importerPreferences);
    }

    @Test
    void getName() {
        assertEquals("BibSonomy", fetcher.getName());
    }

    @Test
    @ExternalServicesTest
    void searchByQueryFindsEntry() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Tang, Chunlei and Jia, Xing and Xiong, Yun")
                .withField(StandardField.TITLE, "From data to value in multimodal learning")
                .withField(StandardField.JOURNAL, "Patterns")
                .withField(StandardField.YEAR, "2026");

        List<BibEntry> fetchedEntries = fetcher.performSearch("From data to value in multimodal learning");

        assertEquals(Optional.of(expected), fetchedEntries.stream().findFirst());
    }

    @Test
    @ExternalServicesTest
    void searchWithNonsenseQueryReturnsEmptyList() throws FetcherException {
        assertEquals(List.of(), fetcher.performSearch("xqzvwplkjhgfdsa1234567890"));
    }

    @Test
    @ExternalServicesTest
    void searchWithEmptyQueryReturnsEmptyList() throws FetcherException {
        assertEquals(List.of(), fetcher.performSearch(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", ":", "user:", ":key", "", "   "})
    void malformedKeyIsInvalid(String key) {
        assertFalse(fetcher.isValidKey(key));
    }

    @Test
    void searchWithoutCredentialsThrows() {
        ImporterPreferences noKeyPreferences = mock(ImporterPreferences.class);
        when(noKeyPreferences.getApiKey(BibSonomyFetcher.FETCHER_NAME)).thenReturn(Optional.empty());
        BibSonomyFetcher fetcherWithoutKey = new BibSonomyFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), noKeyPreferences);

        assertThrows(FetcherException.class, () -> fetcherWithoutKey.performSearch("machine learning"));
    }
}
