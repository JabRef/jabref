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
public class IsbnViaChimboriFetcherTest extends AbstractIsbnFetcherTest {

    @Before
    public void setUp() {
        bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9780321356680");
        bibEntry.setField("title", "Effective Java (Java Series)");
        bibEntry.setField("publisher", "Addison-Wesley Professional");
        bibEntry.setField("year", "2008");
        bibEntry.setField("author", "Joshua Bloch");
        bibEntry.setField("isbn", "978-0321356680");
        bibEntry.setField("url", "https://www.amazon.com/Effective-Java-Joshua-Bloch-ebook/dp/B00B8V09HY%3FSubscriptionId%3D0JYN1NVW651KCA56C102%26tag%3Dtechkie-20%26linkCode%3Dxm2%26camp%3D2025%26creative%3D165953%26creativeASIN%3DB00B8V09HY");

        fetcher = new IsbnViaChimboriFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());
    }

    @Test
    @Override
    public void testName() {
        assertEquals("ISBN (Chimbori/Amazon)", fetcher.getName());
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
        bibEntry.setField("bibtexkey", "0321356683");
        bibEntry.setField("isbn", "0321356683");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "3642434738");
        bibEntry.setField("title", "Fundamentals of Business Process Management");
        bibEntry.setField("publisher", "Springer");
        bibEntry.setField("year", "2015");
        bibEntry.setField("author", "Marlon Dumas and Marcello La Rosa and Jan Mendling and Hajo Reijers");
        bibEntry.setField("isbn", "3642434738");
        bibEntry.setField("url", "https://www.amazon.com/Fundamentals-Business-Process-Management-Marlon/dp/3642434738%3FSubscriptionId%3D0JYN1NVW651KCA56C102%26tag%3Dtechkie-20%26linkCode%3Dxm2%26camp%3D2025%26creative%3D165953%26creativeASIN%3D3642434738");

        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3642434738");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void searchForIsbnAvailableAtChimboriButNonOnEbookDe() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3728128155");
        assertNotEquals(Optional.empty(), fetchedEntry);
    }

}
