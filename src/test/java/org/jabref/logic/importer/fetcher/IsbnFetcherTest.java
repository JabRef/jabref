package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.testutils.category.FetcherTests;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

@Category(FetcherTests.class)
public class IsbnFetcherTest {

    private IsbnFetcher fetcher;
    private BibEntry bibEntry;

    @Before
    public void setUp() {
        fetcher = new IsbnFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

        bibEntry = new BibEntry();
        bibEntry.setType(BiblatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9780321356680");
        bibEntry.setField("title", "Effective Java");
        bibEntry.setField("publisher", "Addison Wesley");
        bibEntry.setField("year", "2008");
        bibEntry.setField("author", "Bloch, Joshua");
        bibEntry.setField("date", "2008-05-08");
        bibEntry.setField("ean", "9780321356680");
        bibEntry.setField("isbn", "0321356683");
        bibEntry.setField("pagetotal", "384");
        bibEntry.setField("url", "http://www.ebook.de/de/product/6441328/joshua_bloch_effective_java.html");
    }

    @Test
    public void testName() {
        assertEquals("ISBN", fetcher.getName());
    }

    @Test
    public void testHelpPage() {
        assertEquals("ISBNtoBibTeX", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void searchByIdSuccessfulWithShortISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0321356683");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("978-0321356680");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void searchByIdReturnsEmptyWithEmptyISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test(expected = FetcherException.class)
    public void searchByIdThrowsExceptionForShortInvalidISBN() throws FetcherException {
        fetcher.performSearchById("123456789");
    }

    @Test(expected = FetcherException.class)
    public void searchByIdThrowsExceptionForLongInvalidISB() throws FetcherException {
        fetcher.performSearchById("012345678910");
    }

    @Test(expected = FetcherException.class)
    public void searchByIdThrowsExceptionForInvalidISBN() throws FetcherException {
        fetcher.performSearchById("jabref-4-ever");
    }

    /**
     * This test searches for a valid ISBN. See https://www.amazon.de/dp/3728128155/?tag=jabref-21
     * However, this ISBN is not available on ebook.de. The fetcher should something as it falls back to Chimbori
     */
    @Test
    public void searchForIsbnAvailableAtChimboriButNonOnEbookDe() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3728128155");
        assertNotEquals(Optional.empty(), fetchedEntry);
    }

}
