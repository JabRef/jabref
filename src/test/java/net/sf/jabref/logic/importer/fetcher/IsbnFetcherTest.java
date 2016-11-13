package net.sf.jabref.logic.importer.fetcher;

import java.util.Optional;

import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IsbnFetcherTest {

    private IsbnFetcher fetcher;
    private BibEntry bibEntry;

    @Before
    public void setUp() {
        fetcher = new IsbnFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());

        bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9780321356680");
        bibEntry.setField("title", "Effective Java");
        bibEntry.setField("publisher", "Addison Wesley");
        bibEntry.setField("year", "2008");
        bibEntry.setField("author", "Joshua Bloch");
        bibEntry.setField("date", "2008-05-08");
        bibEntry.setField("ean", "9780321356680");
        bibEntry.setField("isbn", "0321356683");
        bibEntry.setField("pagetotal", "384");
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
     * However, this ISBN is not available on ebook.de. The fetcher should return nothing rather than throwing an exeption.
     */
    @Test
    public void searchForValidButNotFoundISBN() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3728128155");
        assertEquals(Optional.empty(), fetchedEntry);
    }

}
