package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQueryNode;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
@Disabled("Requires Elsevier/Scopus API key - enable locally with valid key")
class ScopusTest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {

    private static final ImporterPreferences IMPORTER_PREFERENCES = mock(ImporterPreferences.class);
    private static final Optional<String> API_KEY = Optional.of(new BuildInfo().scopusApiKey);

    private Scopus fetcher;
    private BibEntry entry;

    @BeforeAll
    static void ensureScopusIsAvailable() throws FetcherException {
        when(IMPORTER_PREFERENCES.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(IMPORTER_PREFERENCES.getApiKey(ScienceDirect.FETCHER_NAME)).thenReturn(API_KEY);
        Scopus scopus = new Scopus(IMPORTER_PREFERENCES);

        // Skip tests if API is not available
        assumeFalse(List.of().equals(scopus.performSearch("test")));
    }

    @BeforeEach
    void setUp() {
        when(IMPORTER_PREFERENCES.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(IMPORTER_PREFERENCES.getApiKey(ScienceDirect.FETCHER_NAME)).thenReturn(API_KEY);
        fetcher = new Scopus(IMPORTER_PREFERENCES);
        entry = new BibEntry();
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return fetcher;
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return fetcher;
    }

    @Override
    public List<String> getTestAuthors() {
        return List.of("Steinmacher");
    }

    @Override
    public String getTestJournal() {
        return "Information and Software Technology";
    }

    @Override
    public Integer getTestYear() {
        return 2018;
    }

    // ==================== Fetcher Properties Tests ====================

    @Test
    void fetcherName() {
        assertEquals("ScienceDirect", fetcher.getName());
    }

    @Test
    void getTestUrl() {
        String testUrl = fetcher.getTestUrl();
        assertNotNull(testUrl);
        assertTrue(testUrl.contains("api.elsevier.com/content/search/scopus"));
        assertTrue(testUrl.contains("apiKey="));
    }

    // ==================== URL Construction Tests ====================

    @Test
    void urlForQueryContainsRequiredParameters() throws URISyntaxException, MalformedURLException {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.empty(), "machine learning");
        URL url = fetcher.getURLForQuery(queryNode, 0);

        String urlString = url.toString();
        assertTrue(urlString.contains("api.elsevier.com/content/search/scopus"));
        assertTrue(urlString.contains("query="));
        assertTrue(urlString.contains("count="));
        assertTrue(urlString.contains("start="));
    }

    @Test
    void urlForQueryWithPagination() throws URISyntaxException, MalformedURLException {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.empty(), "machine learning");
        URL url = fetcher.getURLForQuery(queryNode, 2);

        String urlString = url.toString();
        // Page 2 with page size 20 should start at 40
        assertTrue(urlString.contains("start=40"));
    }

    // ==================== Search Tests ====================

    @Test
    void searchByQueryFindsEntry() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("machine learning neural networks");

        assertFalse(fetchedEntries.isEmpty());
        // Check that entries have titles
        assertTrue(fetchedEntries.stream()
                                 .anyMatch(e -> e.getField(StandardField.TITLE).isPresent()));
    }

    @Test
    void searchByAuthorFindsEntries() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Steinmacher");

        assertFalse(fetchedEntries.isEmpty());
    }

    @Test
    void searchResultContainsExpectedFields() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("machine learning neural networks");

        assertFalse(fetchedEntries.isEmpty());
        BibEntry firstEntry = fetchedEntries.getFirst();

        // Check that essential fields are populated
        assertTrue(firstEntry.getField(StandardField.TITLE).isPresent());
        // Scopus entries should have DOI or URL
        assertTrue(firstEntry.getField(StandardField.DOI).isPresent() ||
                firstEntry.getField(StandardField.URL).isPresent());
    }

    @Test
    void searchResultContainsJournalInfo() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("software engineering");

        assertFalse(fetchedEntries.isEmpty());
        // At least some entries should have journal information
        assertTrue(fetchedEntries.stream()
                                 .anyMatch(e -> e.getField(StandardField.JOURNAL).isPresent()));
    }
}
