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
import static org.junit.Assert.assertNotEquals;

@Category(FetcherTests.class)
public class IsbnViaChimboriFetcherTest {

    private IsbnViaChimboriFetcher fetcher;
    private BibEntry bibEntry;

    @Before
    public void setUp() {
        fetcher = new IsbnViaChimboriFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());

        bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.BOOK);
        bibEntry.setField("title", "Effective Java (Java Series)");
        bibEntry.setField("publisher", "Addison-Wesley Professional");
        bibEntry.setField("year", "2008");
        bibEntry.setField("author", "Joshua Bloch");
        bibEntry.setField("url", "https://www.amazon.com/Effective-Java-Joshua-Bloch-ebook/dp/B00B8V09HY%3FSubscriptionId%3D0JYN1NVW651KCA56C102%26tag%3Dtechkie-20%26linkCode%3Dxm2%26camp%3D2025%26creative%3D165953%26creativeASIN%3DB00B8V09HY");
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
        bibEntry.setField("bibtexkey", "0321356683");
        bibEntry.setField("isbn", "0321356683");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("978-0321356680");
        bibEntry.setField("bibtexkey", "9780321356680");
        bibEntry.setField("isbn", "978-0321356680");
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

    @Test
    public void searchForIsbnAvailableAtChimboriButNonOnEbookDe() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3728128155");
        assertNotEquals(Optional.empty(), fetchedEntry);
    }

}
