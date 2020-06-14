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
import static org.mockito.Mockito.mock;

@FetcherTest
public class IsbnViaOttoBibFetcherTest extends AbstractIsbnFetcherTest {

    @BeforeEach
    public void setUp() {
        bibEntry = new BibEntry();
        bibEntry.setType(StandardEntryType.Book);
        bibEntry.setCiteKey("bloch2008effective");
        bibEntry.setField(StandardField.TITLE, "Effective Java");
        bibEntry.setField(StandardField.PUBLISHER, "Addison-Wesley");
        bibEntry.setField(StandardField.YEAR, "2008");
        bibEntry.setField(StandardField.AUTHOR, "Bloch, Joshua");
        bibEntry.setField(StandardField.ISBN, "9780321356680");
        bibEntry.setField(StandardField.ADDRESS, "Upper Saddle River, NJ");

        fetcher = new IsbnViaOttoBibFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    @Override
    public void testName() {
        assertEquals("ISBN (OttoBib)", fetcher.getName());
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithShortISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0321356683");
        bibEntry.setField(StandardField.ISBN, "0321356683");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780321356680");
        bibEntry.setField(StandardField.ISBN, "9780321356680");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        bibEntry = new BibEntry();
        bibEntry.setType(StandardEntryType.Book);
        bibEntry.setCiteKey("dumas2018fundamentals");
        bibEntry.setField(StandardField.TITLE, "Fundamentals of business process management");
        bibEntry.setField(StandardField.PUBLISHER, "Springer");
        bibEntry.setField(StandardField.AUTHOR, "Dumas, Marlon");
        bibEntry.setField(StandardField.ADDRESS, "Berlin, Germany");
        bibEntry.setField(StandardField.ISBN, "9783662565094");
        bibEntry.setField(StandardField.YEAR, "2018");

        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("978-3-662-56509-4");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void testISBNNotAvaiableOnEbookDeOrChimbori() throws Exception {
        bibEntry = new BibEntry();
        bibEntry.setType(StandardEntryType.Book);
        bibEntry.setCiteKey("denis2012les");
        bibEntry.setField(StandardField.TITLE, "Les mots du passé : roman");
        bibEntry.setField(StandardField.PUBLISHER, "Éd. les Nouveaux auteurs");
        bibEntry.setField(StandardField.ADDRESS, "Paris");
        bibEntry.setField(StandardField.YEAR, "2012");
        bibEntry.setField(StandardField.AUTHOR, "Denis, ");
        bibEntry.setField(StandardField.ISBN, "9782819502746");

        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("978-2-8195-02746");
        assertEquals(Optional.of(bibEntry), fetchedEntry);

    }

}
