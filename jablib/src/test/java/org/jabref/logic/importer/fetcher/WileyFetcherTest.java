package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class WileyFetcherTest {

    private static final String TEST_API_KEY = "test-token-123";

    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
    private WileyFetcher fetcher;

    @BeforeEach
    void setUp() {
        when(importerPreferences.getApiKey(WileyFetcher.FETCHER_NAME)).thenReturn(Optional.of(TEST_API_KEY));
        fetcher = new WileyFetcher(importerPreferences);
    }

    @Test
    void findFullTextByDoi() throws IOException, FetcherException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1002/we.2952");

        assertEquals(
                Optional.of(URLUtil.create("https://api.wiley.com/onlinelibrary/tdm/v1/articles/10.1002/we.2952")),
                fetcher.findFullText(entry)
        );
    }

    @Test
    void findFullTextReturnsEmptyWithoutApiKey() throws IOException, FetcherException {
        when(importerPreferences.getApiKey(WileyFetcher.FETCHER_NAME)).thenReturn(Optional.empty());
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1002/we.2952");

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextReturnsEmptyWithBlankApiKey() throws IOException, FetcherException {
        when(importerPreferences.getApiKey(WileyFetcher.FETCHER_NAME)).thenReturn(Optional.of("   "));
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1002/we.2952");

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextReturnsEmptyWithoutDoi() throws IOException, FetcherException {
        assertEquals(Optional.empty(), fetcher.findFullText(new BibEntry()));
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.PUBLISHER, fetcher.getTrustLevel());
    }

    @Test
    void downloadHeadersIncludeToken() {
        assertEquals(Map.of("Wiley-TDM-Client-Token", TEST_API_KEY), fetcher.getDownloadHeaders());
    }

    @Test
    void downloadHeadersEmptyWithoutApiKey() {
        when(importerPreferences.getApiKey(WileyFetcher.FETCHER_NAME)).thenReturn(Optional.empty());

        assertTrue(fetcher.getDownloadHeaders().isEmpty());
    }

    @Test
    void validUuidKeyIsAccepted() {
        assertTrue(fetcher.isValidKey("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void invalidKeyIsRejected() {
        assertFalse(fetcher.isValidKey("not-a-uuid"));
    }

    @Test
    void blankKeyIsRejected() {
        assertFalse(fetcher.isValidKey("   "));
    }
}
