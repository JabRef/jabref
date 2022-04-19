package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
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
        bibEntryEffectiveJava = new BibEntry(StandardEntryType.Book)
                .withCitationKey("9780134685991")
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.PUBLISHER, "Addison Wesley")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.DATE, "2018-01-31")
                .withField(new UnknownField("ean"), "9780134685991")
                .withField(StandardField.ISBN, "0134685997")
                .withField(StandardField.URL, "https://www.ebook.de/de/product/28983211/joshua_bloch_effective_java.html");

        fetcher = new IsbnViaEbookDeFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    @Override
    public void testName() {
        assertEquals("ISBN (ebook.de)", fetcher.getName());
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithShortISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0134685997");
        assertEquals(Optional.of(bibEntryEffectiveJava), fetchedEntry);
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780134685991");
        assertEquals(Optional.of(bibEntryEffectiveJava), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("9783662585856")
                .withField(StandardField.TITLE, "Fundamentals of Business Process Management")
                .withField(StandardField.PUBLISHER, "Springer Berlin Heidelberg")
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.AUTHOR, "Dumas, Marlon and Rosa, Marcello La and Mendling, Jan and Reijers, Hajo A.")
                .withField(StandardField.DATE, "2019-02-01")
                .withField(StandardField.PAGETOTAL, "560")
                .withField(new UnknownField("ean"), "9783662585856")
                .withField(StandardField.ISBN, "3662585855")
                .withField(StandardField.URL, "https://www.ebook.de/de/product/35805105/marlon_dumas_marcello_la_rosa_jan_mendling_hajo_a_reijers_fundamentals_of_business_process_management.html");

        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3662585855");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    /**
     * This test searches for a valid ISBN. See https://www.amazon.de/dp/3728128155/?tag=jabref-21 However, this ISBN is
     * not available on ebook.de. The fetcher should return nothing rather than throwing an exception.
     */
    @Test
    public void searchForValidButNotFoundISBN() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3728128155");
        assertEquals(Optional.empty(), fetchedEntry);
    }
}
