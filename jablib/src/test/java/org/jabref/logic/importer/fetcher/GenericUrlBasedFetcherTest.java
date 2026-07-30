package org.jabref.logic.importer.fetcher;

import java.time.LocalDate;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
class GenericUrlBasedFetcherTest {

    private final GenericUrlBasedFetcher fetcher = new GenericUrlBasedFetcher();

    @Test
    void performSearchWithValidUrlReturnsMiscEntryWithTitleAndUrldate() throws FetcherException {
        String url = "https://gi-radar.de/397-coding-unterstuetzung-im-lauf-der-zeit/";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(url, entry.getField(StandardField.URL).orElse(null));
        assertEquals(new Date(LocalDate.now()).getNormalized(), entry.getField(StandardField.URLDATE).orElse(null));
        assertTrue(entry.getField(StandardField.TITLE).map(title -> !title.isBlank()).orElse(false));
    }

    @Test
    void performSearchWithUnreachableUrlStillCreatesEntryWithUrlAsTitleFallback() throws FetcherException {
        String url = "https://this-host-should-not-resolve.jabref-test.invalid/some-page";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(url, entry.getField(StandardField.URL).orElse(null));
        assertEquals(url, entry.getField(StandardField.TITLE).orElse(null));
        assertEquals(new Date(LocalDate.now()).getNormalized(), entry.getField(StandardField.URLDATE).orElse(null));
    }

    @Test
    void performSearchWithSurroundingWhitespaceStripsItFromStoredUrl() throws FetcherException {
        String url = "https://this-host-should-not-resolve.jabref-test.invalid/some-page";

        List<BibEntry> result = fetcher.performSearch(" " + url + " ");

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(url, entry.getField(StandardField.URL).orElse(null));
    }

    @Test
    void performSearchWithInvalidUrlThrowsFetcherException() {
        assertThrows(FetcherException.class, () -> fetcher.performSearch("not a url"));
    }

    @Test
    void performSearchWithBlankInputThrowsFetcherException() {
        assertThrows(FetcherException.class, () -> fetcher.performSearch("   "));
    }

    @Test
    void getName() {
        assertEquals("URL", fetcher.getName());
    }
}
