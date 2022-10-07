package org.jabref.logic.importer.fetcher.isbntobibtex;

import java.util.Optional;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.AbstractIsbnFetcherTest;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class DoiToBibtexConverterComIsbnFetcherTest extends AbstractIsbnFetcherTest {

    @BeforeEach
    public void setUp() {
        bibEntryEffectiveJava = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java(TM) Programming Language Guide (2nd Edition) (The Java Series)")
                .withField(StandardField.PUBLISHER, "Prentice Hall PTR")
                .withField(StandardField.YEAR, "2007")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.ISBN, "9780321356680")
                .withField(StandardField.PAGES, "256");

        fetcher = new DoiToBibtexConverterComIsbnFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    @Override
    public void testName() {
        assertEquals("ISBN (doi-to-bibtex-converter.herokuapp.com)", fetcher.getName());
    }

    @Test
    @Disabled
    @Override
    public void searchByIdSuccessfulWithShortISBN() {
        throw new UnsupportedOperationException();
    }

    @Test
    @Disabled
    @Override
    public void searchByIdSuccessfulWithLongISBN() {
        throw new UnsupportedOperationException();
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Repository")
                .withField(StandardField.ISBN, "9783110702125")
                .withField(StandardField.AUTHOR, "Hans-Joachim Habermann and Frank Leymann")
                .withField(StandardField.PAGES, "294")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DAY, "12")
                .withField(StandardField.MONTH, "10");
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9783110702125");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void testIsbnNeitherAvailable() {
        assertThrows(FetcherClientException.class, () -> fetcher.performSearchById("9785646216541"));
    }

    @Test
    public void searchByIdFailedWithLongISBN() {
        assertThrows(FetcherClientException.class, () -> fetcher.performSearchById("9780321356680"));
    }

    @Test
    public void searchByIdFailedWithShortISBN() throws FetcherException {
        assertThrows(FetcherClientException.class, () -> fetcher.performSearchById("0321356683"));
    }
}
