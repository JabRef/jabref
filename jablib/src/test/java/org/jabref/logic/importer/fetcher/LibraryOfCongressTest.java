package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class LibraryOfCongressTest {

    private LibraryOfCongress fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.bibEntryPreferences()).thenReturn(mock(BibEntryPreferences.class));
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        fetcher = new LibraryOfCongress(importFormatPreferences);
    }

    @Test
    void performSearchById() throws FetcherException {
        BibEntry expected = new BibEntry()
                .withField(StandardField.ADDRESS, "mau, Burlington, MA")
                .withField(StandardField.AUTHOR, "West, Matthew")
                .withField(StandardField.DATE, "2011")
                .withField(StandardField.ISBN, "0123751063 (pbk.)")
                .withField(StandardField.KEYWORDS, "Database design, Data structures (Computer science)")
                .withField(StandardField.LANGUAGE, "eng")
                .withField(new UnknownField("lccn"), "2010045158")
                .withField(StandardField.NOTE, "Matthew West., Includes index.")
                .withField(new UnknownField("oclc"), "ocn665135773")
                .withField(new UnknownField("source"), "aacr")
                .withField(StandardField.TITLE, "Developing high quality data models")
                .withField(StandardField.YEAR, "2011");
        expected.setType(StandardEntryType.Book);

        assertEquals(Optional.of(expected), fetcher.performSearchById("2010045158"));
    }

    @Test
    void parsesAttachedModsBookAsBook() throws IOException, ParseException {
        try (InputStream inputStream = LibraryOfCongressTest.class.getResourceAsStream("library_of_congress_2010045158_mods.xml")) {
            assertTrue(inputStream != null);

            BibEntry expected = new BibEntry()
                    .withField(StandardField.ADDRESS, "mau, Burlington, MA")
                    .withField(StandardField.AUTHOR, "West, Matthew")
                    .withField(StandardField.DATE, "2011")
                    .withField(StandardField.ISBN, "0123751063")
                    .withField(StandardField.KEYWORDS, "Database design, Data structures (Computer science)")
                    .withField(StandardField.LANGUAGE, "eng")
                    .withField(new UnknownField("lccn"), "2010045158")
                    .withField(StandardField.NOTE, "Matthew West., Includes index.")
                    .withField(new UnknownField("oclc"), "665135773")
                    .withField(new UnknownField("source"), "aacr")
                    .withField(StandardField.TITLE, "Developing high quality data models")
                    .withField(StandardField.YEAR, "2011");
            expected.setType(StandardEntryType.Book);

            assertEquals(expected, fetcher.getParser().parseEntries(inputStream).getFirst());
        }
    }

    @Test
    void performSearchByEmptyId() throws FetcherException {
        assertEquals(Optional.empty(), fetcher.performSearchById(""));
    }

    @Test
    void performSearchByInvalidId() {
        assertThrows(FetcherClientException.class, () -> fetcher.performSearchById("xxx"));
    }
}
