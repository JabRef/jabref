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
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class OpenLibraryIsbnFetcherTest extends AbstractIsbnFetcherTest {

    @BeforeEach
    public void setUp() {
        bibEntryEffectiveJava = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java(TM) Programming Language Guide (2nd Edition) (The Java Series)")
                .withField(StandardField.PUBLISHER, "Prentice Hall PTR")
                .withField(StandardField.YEAR, "2007")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.ISBN, "9780321356680")
                .withField(StandardField.PAGES, "256");

        fetcher = new OpenLibraryIsbnFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    @Override
    public void testName() {
        assertEquals("OpenLibrary", fetcher.getName());
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithShortISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0321356683");
        assertEquals(Optional.of(bibEntryEffectiveJava), fetchedEntry);
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780321356680");
        assertEquals(Optional.of(bibEntryEffectiveJava), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Repository Eine Einführung")
                .withField(StandardField.SUBTITLE, "Eine Einführung")
                .withField(StandardField.PUBLISHER, "de Gruyter GmbH, Walter")
                .withField(StandardField.AUTHOR, "Habermann, Hans-Joachim and Leymann, Frank")
                .withField(StandardField.ISBN, "9783110702125")
                .withField(StandardField.YEAR, "2020");
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9783110702125");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    /**
     * Checks whether the given ISBN is <emph>NOT</emph> available at any ISBN fetcher
     */
    @Test
    public void testIsbnNeitherAvailableOnEbookDeNorOrViaOpenLibrary() throws Exception {
        // In this test, the ISBN needs to be a valid (syntax+checksum) ISBN number
        // However, the ISBN number must not be assigned to a real book
        assertThrows(FetcherClientException.class, () -> fetcher.performSearchById("9785646216541"));
    }
}
