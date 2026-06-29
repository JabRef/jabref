package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@FetcherTest
class GenericUrlBasedFetcherTest {

    private GenericUrlBasedFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new GenericUrlBasedFetcher(mock(ImportFormatPreferences.class));
    }

    @Test
    void getName() {
        assertEquals("URL", fetcher.getName());
    }

    @Test
    void blankUrlReturnsEmptyList() throws FetcherException {
        assertEquals(List.of(), fetcher.performSearch("   "));
    }

    @Test
    void malformedUrlThrows() {
        assertThrows(FetcherException.class, () -> fetcher.performSearch("not a valid url"));
    }

    @Test
    void createsMiscEntryWithUrlAndTitle() throws FetcherException {
        List<BibEntry> entries = fetcher.performSearch("https://example.com");

        assertEquals(1, entries.size());
        BibEntry entry = entries.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertTrue(entry.getField(StandardField.URL).orElse("").contains("example.com"));
        assertEquals("Example Domain", entry.getField(StandardField.TITLE).orElse(""));
    }
}
