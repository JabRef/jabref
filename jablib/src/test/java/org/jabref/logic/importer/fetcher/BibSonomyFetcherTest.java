package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.support.ExternalServicesTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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

    /// The live tests need the `BibSonomyUsername` and `BibSonomyApiKey` environment variables at build time and are skipped without them
    private void assumeCredentialsAvailable() {
        assumeTrue(BibSonomyFetcher.hasUsernameAndApiKey(buildInfo.bibSonomyUsername + ":" + buildInfo.bibSonomyApiKey));
    }

    @Test
    void getName() {
        assertEquals("BibSonomy", fetcher.getName());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "learning                          | search=learning&start=0&end=50",
            "learning year=2015                | search=learning&tags=sys%3Ayear%3A2015&start=0&end=50",
            "learning year-range=2010-2012     | search=learning&tags=sys%3Ayear%3A2010-2012&start=0&end=50"
    })
    void yearIsSentAsSystemTag(String query, String expectedParameters) throws MalformedURLException, URISyntaxException {
        SearchQuery searchQuery = new SearchQuery(query);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQuery.getSearchFlags());

        assertEquals("https://www.bibsonomy.org/api/posts?resourcetype=publication&format=bibtex&" + expectedParameters,
                fetcher.getURLForQuery(visitor.visitStart(searchQuery.getContext())).toString());
    }

    @Test
    @ExternalServicesTest
    void searchByQueryFindsEntry() throws FetcherException {
        assumeCredentialsAvailable();
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Tang, Chunlei and Jia, Xing and Xiong, Yun")
                .withField(StandardField.TITLE, "From data to value in multimodal learning.")
                .withField(StandardField.JOURNAL, "Patterns")
                .withField(StandardField.YEAR, "2026")
                .withField(StandardField.VOLUME, "7")
                .withField(StandardField.NUMBER, "8")
                .withField(StandardField.PAGES, "101615")
                .withField(StandardField.DOI, "10.1016/j.patter.2026.101615");
        // Fields such as added-at or interhash belong to the user who posted the entry and may change, so only the bibliographic fields are compared
        List<Field> bibliographicFields = List.copyOf(expected.getFields());

        BibEntry firstEntry = fetcher.performSearch("From data to value in multimodal learning").getFirst();

        assertEquals(expected.getType(), firstEntry.getType());
        assertEquals(bibliographicFields.stream().map(expected::getField).toList(),
                bibliographicFields.stream().map(firstEntry::getField).toList());
    }

    @Test
    @ExternalServicesTest
    void searchWithNonsenseQueryReturnsEmptyList() throws FetcherException {
        assumeCredentialsAvailable();
        assertEquals(List.of(), fetcher.performSearch("xqzvwplkjhgfdsa1234567890"));
    }

    @Test
    @ExternalServicesTest
    void searchWithEmptyQueryReturnsEmptyList() throws FetcherException {
        assumeCredentialsAvailable();
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
