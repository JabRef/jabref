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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class IsbnViaChimboriFetcherTest extends AbstractIsbnFetcherTest {

    @BeforeEach
    public void setUp() {
        bibEntry = new BibEntry();
        bibEntry.setType(BiblatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9780321356680");
        bibEntry.setField("title", "Effective Java (2nd Edition)");
        bibEntry.setField("publisher", "Addison-Wesley");
        bibEntry.setField("year", "2008");
        bibEntry.setField("author", "Joshua Bloch");
        bibEntry.setField("isbn", "978-0321356680");
        bibEntry.setField("url",
                "https://www.amazon.com/Effective-Java-2nd-Joshua-Bloch/dp/0321356683?SubscriptionId=0JYN1NVW651KCA56C102&tag=techkie-20&linkCode=xm2&camp=2025&creative=165953&creativeASIN=0321356683");

        fetcher = new IsbnViaChimboriFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
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
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780321356680");
        bibEntry.setField("bibtexkey", "9780321356680");
        bibEntry.setField("isbn", "9780321356680");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType(BiblatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "3642434738");
        bibEntry.setField("title", "Fundamentals of Business Process Management");
        bibEntry.setField("publisher", "Springer");
        bibEntry.setField("year", "2015");
        bibEntry.setField("author", "Marlon Dumas and Marcello La Rosa and Jan Mendling and Hajo A. Reijers");
        bibEntry.setField("isbn", "3642434738");
        bibEntry.setField("url",
                "https://www.amazon.com/Fundamentals-Business-Process-Management-Marlon/dp/3642434738?SubscriptionId=0JYN1NVW651KCA56C102&tag=techkie-20&linkCode=xm2&camp=2025&creative=165953&creativeASIN=3642434738");

        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3642434738");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void searchForIsbnAvailableAtChimboriButNonOnEbookDe() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3728128155");
        assertNotEquals(Optional.empty(), fetchedEntry);
    }

}
