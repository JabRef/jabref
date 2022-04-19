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
        bibEntryEffectiveJava = new BibEntry(StandardEntryType.Book)
                .withCitationKey("bloch2008effective")
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.YEAR, "2008")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.ISBN, "9780321356680")
                .withField(StandardField.ADDRESS, "Upper Saddle River, NJ");

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
        bibEntryEffectiveJava.setField(StandardField.ISBN, "0321356683");
        assertEquals(Optional.of(bibEntryEffectiveJava), fetchedEntry);
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780321356680");
        bibEntryEffectiveJava.setField(StandardField.ISBN, "9780321356680");
        assertEquals(Optional.of(bibEntryEffectiveJava), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("dumas2018fundamentals")
                .withField(StandardField.TITLE, "Fundamentals of business process management")
                .withField(StandardField.PUBLISHER, "Springer")
                .withField(StandardField.AUTHOR, "Dumas, Marlon")
                .withField(StandardField.ADDRESS, "Berlin, Germany")
                .withField(StandardField.ISBN, "9783662565094")
                .withField(StandardField.YEAR, "2018");
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("978-3-662-56509-4");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    /**
     * Checks whether the given ISBN is <emph>NOT</emph> available at any ISBN fetcher
     */
    @Test
    public void testIsbnNeitherAvailableOnEbookDeNorOrViaChimbori() throws Exception {
        // In this test, the ISBN needs to be a valid (syntax+checksum) ISBN number
        // However, the ISBN number must not be assigned to a real book
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("978-8-8264-2303-6");
        assertEquals(Optional.empty(), fetchedEntry);
    }
}
