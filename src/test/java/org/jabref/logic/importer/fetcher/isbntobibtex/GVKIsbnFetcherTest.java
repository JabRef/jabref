package org.jabref.logic.importer.fetcher.isbntobibtex;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.AbstractIsbnFetcherTest;
import org.jabref.logic.importer.fetcher.GvkFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class GVKIsbnFetcherTest extends AbstractIsbnFetcherTest {

    private BibEntry bibEntryEffectiveJavaLongISBN;

    @BeforeEach
    public void setUp() {
        bibEntryEffectiveJava = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java(TM) Programming Language Guide (2nd Edition) (The Java Series)")
                .withField(StandardField.PUBLISHER, "Prentice Hall PTR")
                .withField(StandardField.YEAR, "2007")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.ISBN, "9780321356680")
                .withField(StandardField.PAGES, "256");

        bibEntryEffectiveJavaLongISBN = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.YEAR, "2011")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.SERIES, "The @Java series")
                .withField(StandardField.ADDRESS, "Upper Saddle River, NJ [u.a.]")
                .withField(StandardField.EDITION, "2. ed., rev. and updated for Java SE 6")
                .withField(StandardField.NOTE, "*Hier auch später erschienene, unveränderte Nachdrucke*")
                .withField(StandardField.ISBN, "9780321356680")
                .withField(StandardField.PAGETOTAL, "346")
                .withField(new UnknownField("ppn_gvk"), "67954951X")
                .withField(StandardField.SUBTITLE, "[revised and updated for Java SE 6]");

        fetcher = new GvkFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    @Override
    public void testName() {
        assertEquals("GVK", fetcher.getName());
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithShortISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0321356683");
        assertEquals(Optional.of(bibEntryEffectiveJavaLongISBN), fetchedEntry);
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780321356680");
        assertEquals(Optional.of(bibEntryEffectiveJavaLongISBN), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Repository")
                .withField(StandardField.SUBTITLE, "Eine Einführung")
                .withField(StandardField.PUBLISHER, "De Gruyter Oldenbourg")
                .withField(StandardField.AUTHOR, "Habermann, Hans-Joachim")
                .withField(StandardField.ISBN, "9783110702125")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.ADDRESS, "München")
                .withField(StandardField.EDITION, "Reprint 2020")
                .withField(StandardField.EDITOR, "Frank Leymann")
                .withField(StandardField.NUMBER, "8.1")
                .withField(StandardField.PAGETOTAL, "1294")
                .withField(StandardField.SERIES, "Handbuch der Informatik")
                .withField(new UnknownField("ppn_gvk"), "1738076555");

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
       assertEquals(Optional.empty(), fetcher.performSearchById("9785646216541"));
    }

    @Test
    void testEResourceIsbnIsReturnedAsBoook() throws Exception {
        assertEquals(Optional.of(StandardEntryType.Book), fetcher.performSearchById("978-0-8229-4557-4").map(BibEntry::getType));
    }
}
