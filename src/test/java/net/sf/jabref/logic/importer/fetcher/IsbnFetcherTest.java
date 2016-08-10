package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

import net.sf.jabref.Globals;
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
        Globals.prefs = JabRefPreferences.getInstance();
        fetcher = new IsbnFetcher();

        bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey","9780321356680");
        bibEntry.setField("title", "Effective Java");
        bibEntry.setField("publisher", "Addison Wesley");
        bibEntry.setField("year", "2008");
        bibEntry.setField("author", "Joshua Bloch");
        bibEntry.setField("date", "2008-05-08");
        bibEntry.setField("ean", "9780321356680");
        bibEntry.setField("isbn", "0321356683");
        bibEntry.setField("pagetotal", "384 Seiten");
        bibEntry.setField("url", "http://www.ebook.de/de/product/6441328/joshua_bloch_effective_java.html");
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
        assertEquals(Optional.of(bibEntry), fetchedEntry.get());

    }

    @Test(expected = NoSuchElementException.class)
    public void testFetcher10FetcherException() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("123456789");
        assertEquals(Optional.empty(), fetchedEntry.get());
    }
}
