package org.jabref.logic.importer.fetcher.isbntobibtex;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
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

        bibEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.PUBLISHER, "Addison-Wesley Professional")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.PAGES, "416")
                .withField(StandardField.ISBN, "9780134685991");
    }

    @Test
    void testName() {
        assertEquals("ISBN", fetcher.getName());
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
        BibEntry input = new BibEntry().withField(StandardField.ISBN, "0134685997");

        List<BibEntry> fetchedEntry = fetcher.performSearch(input);
        assertEquals(Collections.singletonList(bibEntry), fetchedEntry);
    }

    /**
     * This test searches for a valid ISBN. See https://www.amazon.de/dp/3728128155/?tag=jabref-21 However, this ISBN is
     * not available on ebook.de. The fetcher should something as it falls back to OttoBib
     */
    @Test
    void searchForIsbnAvailableAtOttoBibButNonOnEbookDe() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3728128155");
        assertNotEquals(Optional.empty(), fetchedEntry);
    }
}
