package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class LibraryOfCongressTest {

    private LibraryOfCongress fetcher;

    @BeforeEach
    public void setUp() {
        ImportFormatPreferences prefs = mock(ImportFormatPreferences.class);
        when(prefs.getKeywordSeparator()).thenReturn(',');
        fetcher = new LibraryOfCongress(prefs);
    }

    @Test
    public void performSearchById() throws Exception {
        BibEntry expected = new BibEntry()
                .withField(StandardField.ADDRESS, "Burlington, MA")
                .withField(StandardField.AUTHOR, "West, Matthew")
                .withField(StandardField.ISBN, "0123751063 (pbk.)")
                .withField(new UnknownField("issuance"), "monographic")
                .withField(StandardField.KEYWORDS, "Database design, Data structures (Computer science)")
                .withField(StandardField.LANGUAGE, "eng")
                .withField(new UnknownField("lccn"), "2010045158")
                .withField(StandardField.NOTE, "Matthew West., Includes index.")
                .withField(new UnknownField("oclc"), "ocn665135773")
                .withField(StandardField.PUBLISHER, "Morgan Kaufmann")
                .withField(new UnknownField("source"), "DLC")
                .withField(StandardField.TITLE, "Developing high quality data models")
                .withField(StandardField.YEAR, "2011");

        assertEquals(Optional.of(expected), fetcher.performSearchById("2010045158"));
    }

    @Test
    public void performSearchByEmptyId() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById(""));
    }

    @Test
    public void performSearchByInvalidId() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById("xxx"));
    }
}
