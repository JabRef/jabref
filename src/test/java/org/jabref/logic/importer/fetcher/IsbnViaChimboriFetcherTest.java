package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
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
        bibEntry.setType(StandardEntryType.Book);
        bibEntry.setCiteKey("9780321356680");
        bibEntry.setField(StandardField.TITLE, "Effective Java (2nd Edition)");
        bibEntry.setField(StandardField.PUBLISHER, "Addison-Wesley");
        bibEntry.setField(StandardField.YEAR, "2008");
        bibEntry.setField(StandardField.AUTHOR, "Joshua Bloch");
        bibEntry.setField(StandardField.ISBN, "978-0321356680");
        bibEntry.setField(StandardField.URL,
                          "https://www.amazon.com/Effective-Java-2nd-Joshua-Bloch/dp/0321356683?SubscriptionId=AKIAIOBINVZYXZQZ2U3A&tag=chimbori05-20&linkCode=xm2&camp=2025&creative=165953&creativeASIN=0321356683");

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
        assertEquals("ISBNtoBibTeX", fetcher.getHelpPage().get().getPageName());
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithShortISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0321356683");
        bibEntry.setCiteKey("0321356683");
        bibEntry.setField(StandardField.ISBN, "0321356683");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780321356680");
        bibEntry.setCiteKey("9780321356680");
        bibEntry.setField(StandardField.ISBN, "9780321356680");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType(StandardEntryType.Book);
        bibEntry.setCiteKey("3642434738");
        bibEntry.setField(StandardField.TITLE, "Fundamentals of Business Process Management");
        bibEntry.setField(StandardField.PUBLISHER, "Springer");
        bibEntry.setField(StandardField.YEAR, "2015");
        bibEntry.setField(StandardField.AUTHOR, "Marlon Dumas and Marcello La Rosa and Jan Mendling and Hajo A. Reijers");
        bibEntry.setField(StandardField.ISBN, "3642434738");
        bibEntry.setField(StandardField.URL,
                          "https://www.amazon.com/Fundamentals-Business-Process-Management-Marlon/dp/3642434738?SubscriptionId=AKIAIOBINVZYXZQZ2U3A&tag=chimbori05-20&linkCode=xm2&camp=2025&creative=165953&creativeASIN=3642434738");

        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3642434738");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void searchForIsbnAvailableAtChimboriButNonOnEbookDe() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3728128155");
        assertNotEquals(Optional.empty(), fetchedEntry);
    }
}
