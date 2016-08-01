package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IsbnFetcherTest {

    private IsbnFetcher fetcher;

    @Before
    public void setup(){
        Globals.prefs = JabRefPreferences.getInstance();
        fetcher = new IsbnFetcher();
    }

    @Test
    public void testName(){
        assertEquals("ISBN", fetcher.getName());
    }

    @Test
    public void testHelpPage(){
        assertEquals("ISBNtoBibTeXHelp", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void testFetcher10() throws FetcherException, IOException {
        Optional<BibEntry> isbn = fetcher.performSearchById("0321356683");

        try (InputStream bibStream = IsbnFetcherTest.class.getResourceAsStream("IsbnFetcherTest.bib")) {
            BibEntryAssert.assertEquals(bibStream, isbn.get());
        }

    }

    @Test
    public void testFetcher13() throws FetcherException, IOException {
        Optional<BibEntry> isbn = fetcher.performSearchById("978-0321356680");

        try (InputStream bibStream = IsbnFetcherTest.class.getResourceAsStream("IsbnFetcherTest.bib")) {
            BibEntryAssert.assertEquals(bibStream, isbn.get());
        }

    }
}
