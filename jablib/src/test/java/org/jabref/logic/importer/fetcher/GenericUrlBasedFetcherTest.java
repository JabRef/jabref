package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

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
    void getNameReturnsURL() {
        assertEquals("Generic URL Fetcher", fetcher.getName());
    }

    @Test
    void fetchEntryFromUrlWithValidUrlCreatesCorrectEntry() {
        Optional<BibEntry> result = fetcher.fetchEntryFromUrl(TEST_URL);

        assertTrue(result.isPresent());

        BibEntry entry = result.get();
        assertEquals(Optional.of(TEST_URL), entry.getField(StandardField.URL));
        assertEquals(StandardEntryType.Misc, entry.getType());
    }

    @Test
    void performSearchWithBlankUrlReturnsEmptyList() {
        List<BibEntry> results = fetcher.performSearch("   ");

        assertTrue(results.isEmpty());
    }

    @Test
    void performSearchTrimsUrl() {
        List<BibEntry> results = fetcher.performSearch("  " + TEST_URL + "  ");

        assertEquals(Optional.of(TEST_URL), results.get(0).getField(StandardField.URL));
    }
}
