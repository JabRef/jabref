package org.jabref.logic.importer.fetcher.isbntobibtex;

import java.util.Optional;

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
import static org.mockito.Mockito.mock;

class LOBIDIsbnFetcherTest extends AbstractIsbnFetcherTest {

    private BibEntry bibEntryEffectiveJavaLongISBN;

    @BeforeEach
    void setUp() {
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
                .withField(StandardField.DATE, "2015")
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.LANGUAGE, "Englisch")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.LOCATION, "Upper Saddle River, NJ [u.a.]")
                .withField(StandardField.EDITION, "2. ed")
                .withField(StandardField.ISBN, "9780321356680")
                .withField(StandardField.TYPE, "BibliographicResource, Book")
                .withField(StandardField.KEYWORDS, "Java Standard Edition 6, Java <Programmiersprache>")
                .withField(StandardField.URL, "http://lobid.org/resources/991000506229708980")
                .withField(StandardField.TITLEADDON, "[revised and updated for Java SE 6]");

        fetcher = new LOBIDIsbnFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Override
    @Test
    public void testName() {
        assertEquals("LOBID", fetcher.getName());
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws FetcherException {
        BibEntry article = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Nichols, Catherine and Blume, Eugen and Hamburger Bahnhof - Museum für Gegenwart - Berlin and Nationalgalerie (Berlin) and DruckVerlag Kettler GmbH")
                .withField(StandardField.ABSTRACT, "Impresum: \"Diese Publikation erscheint anlässlich der Ausstellung Das Kapital. Schuld-Territorium-Utopie. Eine Ausstellung der Nationalgalerie im Hamburger Bahnhof - Museum für Gegenwart - Berlin, 2. Juli-6. November 2016\"")
                .withField(StandardField.PUBLISHER, "Verlag Kettler")
                .withField(StandardField.DATE, "2016")
                .withField(StandardField.EDITION, "1. Auflage")
                .withField(StandardField.ISBN, "9783862065752")
                .withField(StandardField.KEYWORDS, "Cathrine Nichols, Eugen Blume, Staatliche Museen zu Berlin, Beuys, Joseph: Das Kapital Raum 1970-1977, Kunst, Kapitalismus (Motiv), Geschichte")
                .withField(StandardField.LANGUAGE, "Deutsch")
                .withField(StandardField.LOCATION, "Dortmund")
                .withField(StandardField.TITLE, "Das Kapital")
                .withField(StandardField.TITLEADDON, "Schuld - Territorium - Utopie")
                .withField(StandardField.TYPE, "BibliographicResource, Book")
                .withField(StandardField.URL, "http://lobid.org/resources/990212549810206441")
                .withField(StandardField.YEAR, "2016");

        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9783862065752");
        assertEquals(Optional.of(article), fetchedEntry);
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
}
