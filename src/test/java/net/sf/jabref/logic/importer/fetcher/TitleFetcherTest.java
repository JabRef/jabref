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

@Category(FetcherTests.class)
public class TitleFetcherTest {

    private TitleFetcher fetcher;
    private BibEntry bibEntryBischof2009;

    @Before
    public void setUp() {
        fetcher = new TitleFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());

        bibEntryBischof2009 = new BibEntry();
        bibEntryBischof2009.setType(BibLatexEntryTypes.INPROCEEDINGS);
        bibEntryBischof2009.setField("bibtexkey", "Bischof_2009");
        bibEntryBischof2009.setField("author", "Marc Bischof and Oliver Kopp and Tammo van Lessen and Frank Leymann");
        bibEntryBischof2009.setField("booktitle", "2009 35th Euromicro Conference on Software Engineering and Advanced Applications");
        bibEntryBischof2009.setField("publisher", "Institute of Electrical and Electronics Engineers ({IEEE})");
        bibEntryBischof2009.setField("title", "{BPELscript}: A Simplified Script Syntax for {WS}-{BPEL} 2.0");
        bibEntryBischof2009.setField("year", "2009");
        bibEntryBischof2009.setField("doi", "10.1109/seaa.2009.21");
    }

    @Test
    public void testGetName() {
        assertEquals("Title", fetcher.getName());
    }

    @Test
    public void testGetHelpPage() {
        assertEquals("TitleToBibTeX", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void testPerformSearchKopp2007() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("BPELscript: A simplified script syntax for WS-BPEL 2.0");
        assertEquals(Optional.of(bibEntryBischof2009), fetchedEntry);
    }

    @Test
    public void testPerformSearchEmptyTitle() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testPerformSearchInvalidTitle() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("An unknown title where noi DOI can be determined");
        assertEquals(Optional.empty(), fetchedEntry);
    }

}
