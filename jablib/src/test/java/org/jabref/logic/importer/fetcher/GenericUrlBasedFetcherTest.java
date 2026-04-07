package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericUrlBasedFetcherTest {

    private static final String TEST_URL = "https://gi-radar.de/397-coding-unterstuetzung-im-lauf-der-zeit/";

    private GenericUrlBasedFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new GenericUrlBasedFetcher();
    }

    @Test
    void getNameReturnsCorrectName() {
        assertEquals("Generic URL Fetcher", fetcher.getName());
    }

    @Test
    void fetchEntryFromUrlWithValidUrlCreatesCorrectEntry() throws FetcherException {
        List<BibEntry> results = fetcher.fetchEntryFromUrl(TEST_URL);

        assertEquals(1, results.size());

        BibEntry entry = results.get(0);

        assertEquals(TEST_URL, entry.getField(StandardField.URL).orElse(""));
        assertEquals(StandardEntryType.Misc, entry.getType());
    }

    @Test
    void fetchEntryFromUrlWithBlankUrlReturnsEmptyList() throws FetcherException {
        List<BibEntry> results = fetcher.fetchEntryFromUrl("   ");

        assertTrue(results.isEmpty());
    }

    @Test
    void fetchEntryFromUrlTrimsUrl() throws FetcherException {
        List<BibEntry> results = fetcher.fetchEntryFromUrl("  " + TEST_URL + "  ");

        assertEquals(1, results.size());
        assertEquals(TEST_URL, results.get(0).getField(StandardField.URL).orElse(""));
    }
}
