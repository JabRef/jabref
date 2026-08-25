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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenericUrlBasedFetcherTest {

    private final GenericUrlBasedFetcher fetcher = new GenericUrlBasedFetcher();

    @Test
    @FetcherTest
    void performSearchWithValidUrlReturnsMiscEntryWithTitleAndUrldate() throws FetcherException {
        String url = "https://gi-radar.de/397-coding-unterstuetzung-im-lauf-der-zeit/";
        // Captured before performSearch, which internally calls LocalDate.now() itself during its (real, network-
        // dependent) title fetch -- capturing after the call risks the two calls straddling a midnight rollover.
        String expectedUrlDate = new Date(LocalDate.now()).getNormalized();

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(url, entry.getField(StandardField.URL).orElse(null));
        assertEquals(expectedUrlDate, entry.getField(StandardField.URLDATE).orElse(null));
        // The title must be the one scraped from the page -- if the fetch failed, the fetcher falls back to using
        // the URL itself as the title, which this test must not accept as a pass.
        assertNotEquals(url, entry.getField(StandardField.TITLE).orElseThrow());
    }

    @Test
    @FetcherTest
    void performSearchWithUnreachableUrlStillCreatesEntryWithUrlAsTitleFallback() throws FetcherException {
        String url = "https://this-host-should-not-resolve.jabref-test.invalid/some-page";
        String expectedUrlDate = new Date(LocalDate.now()).getNormalized();

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(url, entry.getField(StandardField.URL).orElse(null));
        assertEquals(url, entry.getField(StandardField.TITLE).orElse(null));
        assertEquals(expectedUrlDate, entry.getField(StandardField.URLDATE).orElse(null));
    }

    @Test
    @FetcherTest
    void performSearchWithSurroundingWhitespaceStripsItFromStoredUrl() throws FetcherException {
        String url = "https://this-host-should-not-resolve.jabref-test.invalid/some-page";

        List<BibEntry> result = fetcher.performSearch(" " + url + " ");

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(url, entry.getField(StandardField.URL).orElse(null));
    }

    @Test
    void performSearchWithNonHttpUrlFallsBackToUrlAsTitle() throws FetcherException {
        // URLUtil.isURL accepts ftp:// (its regex allows https?|ftp), but jsoup only supports http/https: its
        // protocol check throws MalformedURLException (an IOException) before opening any connection, which
        // fetchTitle must treat like any other fetch failure instead of aborting entry creation.
        String url = "ftp://example.com/some-file";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(url, entry.getField(StandardField.URL).orElse(null));
        assertEquals(url, entry.getField(StandardField.TITLE).orElse(null));
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
