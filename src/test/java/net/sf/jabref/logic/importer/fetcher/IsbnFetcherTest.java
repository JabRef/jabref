package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.util.Optional;

import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IsbnFetcherTest {

    private IsbnFetcher fetcher;
    private BibEntry bibEntry;

    @Before
    public void setUp() {
        fetcher = new IsbnFetcher();

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
        assertEquals("ISBNtoBibTeXHelp", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void testFetcher10() throws FetcherException, IOException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0321356683");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void testFetcher13() throws FetcherException, IOException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("978-0321356680");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void testFetcher10Empty() throws FetcherException, IOException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testFetcher10ShortISBN() throws FetcherException, IOException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("123456789");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testFetcher10LongISBN() throws FetcherException, IOException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("012345678910");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testFetcher10InvalidISBN() throws FetcherException, IOException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("jabref-4-ever");
        assertEquals(Optional.empty(), fetchedEntry);
    }
}
