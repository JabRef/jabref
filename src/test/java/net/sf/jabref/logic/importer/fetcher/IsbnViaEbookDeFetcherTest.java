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
public class IsbnViaEbookDeFetcherTest extends AbstractIsbnFetcherTest {

    @Before
    public void setUp() {
        bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.BOOK);
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

        fetcher = new IsbnViaEbookDeFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());
    }

    @Test
    @Override
    public void testName() {
        assertEquals("ISBN (ebook.de)", fetcher.getName());
    }

    @Test
    @Override
    public void testHelpPage() {
        assertEquals("ISBNtoBibTeX", fetcher.getHelpPage().getPageName());
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithShortISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0321356683");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9783642434730");
        bibEntry.setField("title", "Fundamentals of Business Process Management");
        bibEntry.setField("publisher", "Springer");
        bibEntry.setField("year", "2015");
        bibEntry.setField("author", "Dumas, Marlon and Rosa, Marcello La and Mendling, Jan and Reijers, Hajo");
        bibEntry.setField("date", "2015-04-12");
        bibEntry.setField("ean", "9783642434730");
        bibEntry.setField("isbn", "3642434738");
        bibEntry.setField("pagetotal", "428");
        bibEntry.setField("url", "http://www.ebook.de/de/product/23955263/marlon_dumas_marcello_la_rosa_jan_mendling_hajo_reijers_fundamentals_of_business_process_management.html");

        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3642434738");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
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
