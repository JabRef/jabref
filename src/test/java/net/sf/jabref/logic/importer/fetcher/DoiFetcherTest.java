package net.sf.jabref.logic.importer.fetcher;

import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DoiFetcherTest {

    private DoiFetcher fetcher;
    private BibEntry bibEntry;

    @Before
    public void setUp() {
        fetcher = new DoiFetcher(Globals.prefs.getImportFormatPreferences());

        bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "Burd_2011");
        bibEntry.setField("title", "Java{\\textregistered} For Dummies{\\textregistered}");
        bibEntry.setField("publisher", "Wiley-Blackwell");
        bibEntry.setField("year", "2011");
        bibEntry.setField("author", "Barry Burd");
        bibEntry.setField("month", "jul");
        bibEntry.setField("doi", "10.1002/9781118257517");
        bibEntry.setField("url", "http://dx.doi.org/10.1002/9781118257517");
    }


    @Test
    public void testGetName() {
        assertEquals("DOI to BibTeX", fetcher.getName());
    }

    @Test
    public void testGetHelpPage() {
        assertEquals("DOItoBibTeXHelp", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void testPerformSearch() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1002/9781118257517");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void testPerformSearchEmpty() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test(expected = FetcherException.class)
    public void testPerformSearchFetcherException() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1002/9781118257517F");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test(expected = NullPointerException.class)
    public void testGetEntryFromDOILongNullPointerException() throws FetcherException {
        assertEquals(Optional.empty(), DoiFetcher.getEntryFromDOI("10.1002/9781118257517F", null, Globals.prefs.getImportFormatPreferences()));
    }

    @Test
    public void testGetEntryFromDOILong() throws FetcherException {
        assertEquals(Optional.of(bibEntry), DoiFetcher.getEntryFromDOI("10.1002/9781118257517", new ParserResult(), Globals.prefs.getImportFormatPreferences()));
    }

}
