package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@FetcherTest
class IsbnFetcherTest {

    private IsbnFetcher fetcher;
    private BibEntry bibEntry;

    @BeforeEach
    void setUp() {
        fetcher = new IsbnFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

        bibEntry = new BibEntry();
        bibEntry.setType(BiblatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9780134685991");
        bibEntry.setField("title", "Effective Java");
        bibEntry.setField("publisher", "Addison Wesley");
        bibEntry.setField("year", "2018");
        bibEntry.setField("author", "Bloch, Joshua");
        bibEntry.setField("date", "2018-01-11");
        bibEntry.setField("ean", "9780134685991");
        bibEntry.setField("isbn", "0134685997");
        bibEntry.setField("url", "https://www.ebook.de/de/product/28983211/joshua_bloch_effective_java.html");
    }

    @Test
    void testName() {
        assertEquals("ISBN", fetcher.getName());
    }

    @Test
    void testHelpPage() {
        assertEquals("ISBNtoBibTeX", fetcher.getHelpPage().get().getPageName());
    }

    @Test
    void searchByIdSuccessfulWithShortISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0134685997");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780134685991");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    void searchByIdReturnsEmptyWithEmptyISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    void searchByIdThrowsExceptionForShortInvalidISBN() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("123456789"));
    }

    @Test
    void searchByIdThrowsExceptionForLongInvalidISB() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("012345678910"));
    }

    @Test
    void searchByIdThrowsExceptionForInvalidISBN() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("jabref-4-ever"));
    }

    @Test
    void searchByEntryWithISBNSuccessful() throws FetcherException {
        BibEntry input = new BibEntry().withField("isbn", "0134685997");

        List<BibEntry> fetchedEntry = fetcher.performSearch(input);
        assertEquals(Collections.singletonList(bibEntry), fetchedEntry);
    }

    /**
     * This test searches for a valid ISBN. See https://www.amazon.de/dp/3728128155/?tag=jabref-21 However, this ISBN is
     * not available on ebook.de. The fetcher should something as it falls back to Chimbori
     */
    @Test
    void searchForIsbnAvailableAtChimboriButNonOnEbookDe() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3728128155");
        assertNotEquals(Optional.empty(), fetchedEntry);
    }
}
