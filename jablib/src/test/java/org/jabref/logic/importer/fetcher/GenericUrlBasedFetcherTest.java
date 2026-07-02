package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GenericUrlBasedFetcherTest {

    private static final ImportFormatPreferences PREFERENCES = mock(ImportFormatPreferences.class);

    /// Builds a fetcher whose download seam returns fixed content, so the tests never touch the network.
    private static GenericUrlBasedFetcher fetcherServing(String pageContent) {
        return new GenericUrlBasedFetcher(PREFERENCES, _ -> Optional.ofNullable(pageContent));
    }

    @Test
    void getName() {
        assertEquals("URL", fetcherServing(null).getName());
    }

    @Test
    void blankUrlReturnsEmptyList() throws FetcherException {
        assertEquals(List.of(), fetcherServing(null).performSearch("   "));
    }

    @Test
    void malformedUrlThrows() {
        assertThrows(FetcherException.class, () -> fetcherServing(null).performSearch("not a valid url"));
    }

    @Test
    void createsMiscEntryWithUrlAndTitle() throws FetcherException {
        GenericUrlBasedFetcher fetcher = fetcherServing("<html><head><title>Example Domain</title></head></html>");

        List<BibEntry> entries = fetcher.performSearch("https://example.com");

        assertEquals(1, entries.size());
        BibEntry entry = entries.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertTrue(entry.getField(StandardField.URL).orElse("").contains("example.com"));
        assertEquals("Example Domain", entry.getField(StandardField.TITLE).orElse(""));
    }

    @Test
    void titleWhitespaceIsCollapsed() throws FetcherException {
        GenericUrlBasedFetcher fetcher = fetcherServing("<title>\n  Spaced   out\ttitle\n</title>");

        BibEntry entry = fetcher.performSearch("https://example.com").getFirst();

        assertEquals("Spaced out title", entry.getField(StandardField.TITLE).orElse(""));
    }

    @Test
    void entryHasNoTitleWhenPageHasNone() throws FetcherException {
        GenericUrlBasedFetcher fetcher = fetcherServing("<html><head></head><body>No title here</body></html>");

        BibEntry entry = fetcher.performSearch("https://example.com").getFirst();

        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
        assertTrue(entry.getField(StandardField.URL).orElse("").contains("example.com"));
    }

    @Test
    void downloadFailureStillYieldsEntryWithUrl() throws FetcherException {
        GenericUrlBasedFetcher fetcher = new GenericUrlBasedFetcher(PREFERENCES, _ -> {
            throw new FetcherException("Simulated download failure");
        });

        BibEntry entry = fetcher.performSearch("https://example.com").getFirst();

        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
        assertTrue(entry.getField(StandardField.URL).orElse("").contains("example.com"));
    }
}
