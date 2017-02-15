package net.sf.jabref.logic.importer.fetcher;

import java.util.Optional;

import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.testutils.category.FetcherTests;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Category(FetcherTests.class)
public class DoiFetcherTest {

    private DoiFetcher fetcher;
    private BibEntry bibEntryBurd2011, bibEntryDecker2007;

    @Before
    public void setUp() {
        fetcher = new DoiFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());

        bibEntryBurd2011 = new BibEntry();
        bibEntryBurd2011.setType(BibLatexEntryTypes.BOOK);
        bibEntryBurd2011.setField("bibtexkey", "Burd_2011");
        bibEntryBurd2011.setField("title", "Java{\\textregistered} For Dummies{\\textregistered}");
        bibEntryBurd2011.setField("publisher", "Wiley-Blackwell");
        bibEntryBurd2011.setField("year", "2011");
        bibEntryBurd2011.setField("author", "Barry Burd");
        bibEntryBurd2011.setField("month", "jul");
        bibEntryBurd2011.setField("doi", "10.1002/9781118257517");

        bibEntryDecker2007 = new BibEntry();
        bibEntryDecker2007.setType(BibLatexEntryTypes.INPROCEEDINGS);
        bibEntryDecker2007.setField("bibtexkey", "Decker_2007");
        bibEntryDecker2007.setField("author", "Gero Decker and Oliver Kopp and Frank Leymann and Mathias Weske");
        bibEntryDecker2007.setField("booktitle", "{IEEE} International Conference on Web Services ({ICWS} 2007)");
        bibEntryDecker2007.setField("month", "jul");
        bibEntryDecker2007.setField("publisher", "Institute of Electrical and Electronics Engineers ({IEEE})");
        bibEntryDecker2007.setField("title", "{BPEL}4Chor: Extending {BPEL} for Modeling Choreographies");
        bibEntryDecker2007.setField("year", "2007");
        bibEntryDecker2007.setField("doi", "10.1109/icws.2007.59");
    }

    @Test
    public void testGetName() {
        assertEquals("DOI", fetcher.getName());
    }

    @Test
    public void testGetHelpPage() {
        assertEquals("DOItoBibTeX", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void testPerformSearchBurd2011() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1002/9781118257517");
        assertEquals(Optional.of(bibEntryBurd2011), fetchedEntry);
    }

    @Test
    public void testPerformSearchDecker2007() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1109/ICWS.2007.59");
        assertEquals(Optional.of(bibEntryDecker2007), fetchedEntry);
    }

    @Test(expected = FetcherException.class)
    public void testPerformSearchEmptyDOI() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test(expected = FetcherException.class)
    public void testPerformSearchInvalidDOI() throws FetcherException {
        fetcher.performSearchById("10.1002/9781118257517F");
        fail();
    }
}
