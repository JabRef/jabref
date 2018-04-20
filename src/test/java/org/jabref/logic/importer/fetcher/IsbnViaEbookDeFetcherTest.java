package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class IsbnViaEbookDeFetcherTest extends AbstractIsbnFetcherTest {

    @BeforeEach
    public void setUp() {
        bibEntry = new BibEntry();
        bibEntry.setType(BiblatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9780134685991");
        bibEntry.setField("title", "Effective Java");
        bibEntry.setField("publisher", "Addison Wesley");
        bibEntry.setField("year", "2018");
        bibEntry.setField("author", "Bloch, Joshua");
        bibEntry.setField("date", "2018-01-11");
        bibEntry.setField("ean", "9780134685991");
        bibEntry.setField("isbn", "0134685997");
        bibEntry.setField("url", "https://www.ebook.de/de/product/28983211/joshua_bloch_effective_java.html");

        fetcher = new IsbnViaEbookDeFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
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
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0134685997");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780134685991");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType(BiblatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9783642434730");
        bibEntry.setField("title", "Fundamentals of Business Process Management");
        bibEntry.setField("publisher", "Springer Berlin Heidelberg");
        bibEntry.setField("year", "2015");
        bibEntry.setField("author", "Dumas, Marlon and Rosa, Marcello La and Mendling, Jan and Reijers, Hajo A.");
        bibEntry.setField("date", "2015-04-12");
        bibEntry.setField("ean", "9783642434730");
        bibEntry.setField("isbn", "3642434738");
        bibEntry.setField("pagetotal", "428");
        bibEntry.setField("url", "https://www.ebook.de/de/product/23955263/marlon_dumas_marcello_la_rosa_jan_mendling_hajo_a_reijers_fundamentals_of_business_process_management.html");

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
