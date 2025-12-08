package org.jabref.logic.importer.fetcher;

import java.io.IOException;
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
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQueryNode;
import org.jabref.support.DisabledOnCIServer;
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
class ScienceDirectTest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {

    private static final ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
    private static final Optional<String> API_KEY = Optional.of(new BuildInfo().scienceDirectApiKey);

    private ScienceDirect fetcher;
    private BibEntry entry;

    @BeforeAll
    static void ensureScopusIsAvailable() throws FetcherException {
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importerPreferences.getApiKey(ScienceDirect.FETCHER_NAME)).thenReturn(API_KEY);
        ScienceDirect scienceDirect = new ScienceDirect(importerPreferences);

        // Skip tests if API is not available
        assumeFalse(List.of().equals(scienceDirect.performSearch("test")));
    }

    @BeforeEach
    void setUp() {
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importerPreferences.getApiKey(ScienceDirect.FETCHER_NAME)).thenReturn(API_KEY);
        fetcher = new ScienceDirect(importerPreferences);
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

    // ==================== Fulltext Fetcher Tests ====================

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDoiOldPage() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.jrmge.2015.08.004");

        assertEquals(
                Optional.of(URLUtil.create("https://www.sciencedirect.com/science/article/pii/S1674775515001079/pdfft?md5=2b19b19a387cffbae237ca6a987279df&pid=1-s2.0-S1674775515001079-main.pdf")),
                fetcher.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDoiNewPage() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.aasri.2014.09.002");

        assertEquals(
                Optional.of(URLUtil.create("https://www.sciencedirect.com/science/article/pii/S2212671614001024/pdf?md5=4e2e9a369b4d5b3db5100aba599bef8b&pid=1-s2.0-S2212671614001024-main.pdf")),
                fetcher.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDoiWorksForBoneArticle() throws IOException {
        // The DOI is an example by a user taken from https://github.com/JabRef/jabref/issues/5860
        entry.setField(StandardField.DOI, "https://doi.org/10.1016/j.bone.2020.115226");

        assertEquals(
                Optional.of(URLUtil.create("https://www.sciencedirect.com/science/article/pii/S8756328220300065/pdfft?md5=0ad75ff155637dec358e5c9fb8b90afd&pid=1-s2.0-S8756328220300065-main.pdf")),
                fetcher.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void notFoundByDoi() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.aasri.2014.0559.002");

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    // ==================== Fetcher Properties Tests ====================

    @Test
    void testFetcherName() {
        assertEquals("ScienceDirect", fetcher.getName());
    }

    @Test
    void testGetTestUrl() {
        String testUrl = fetcher.getTestUrl();
        assertNotNull(testUrl);
        assertTrue(testUrl.contains("api.elsevier.com/content/search/scopus"));
        assertTrue(testUrl.contains("apiKey="));
    }

    @Test
    void testTrustLevel() {
        // ScienceDirect is a publisher source
        assertNotNull(fetcher.getTrustLevel());
    }

    // ==================== URL Construction Tests ====================

    @Test
    void testUrlForQueryContainsRequiredParameters() throws URISyntaxException, MalformedURLException {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.empty(), "machine learning");
        URL url = fetcher.getURLForQuery(queryNode, 0);

        String urlString = url.toString();
        assertTrue(urlString.contains("api.elsevier.com/content/search/scopus"));
        assertTrue(urlString.contains("query="));
        assertTrue(urlString.contains("count="));
        assertTrue(urlString.contains("start="));
    }

    @Test
    void testUrlForQueryWithPagination() throws URISyntaxException, MalformedURLException {
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
