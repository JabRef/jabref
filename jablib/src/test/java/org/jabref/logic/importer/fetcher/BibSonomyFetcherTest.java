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
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExternalServicesTest
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
    /* To check the name shown in JabRef's dropdown is "BibSonomy" */
    void getName() {
        assertEquals("BibSonomy", fetcher.getName());
    }

    @Test
    /* To check to real search returns the correct paper, field by field */
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
    /* To check a search with no matches gives an empty list, not an error */
    void searchWithNonsenseQueryReturnsEmptyList() throws FetcherException {
        assertEquals(List.of(), fetcher.performSearch("xqzvwplkjhgfdsa1234567890"));
    }

    @Test
    /* To check an empty search box gives an empty list */
    void searchWithEmptyQueryReturnsEmptyList() throws FetcherException {
        assertEquals(List.of(), fetcher.performSearch(""));
    }
}
