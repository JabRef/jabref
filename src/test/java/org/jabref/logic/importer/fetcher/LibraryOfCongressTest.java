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
        BibEntry expected = new BibEntry();
        expected.setField(StandardField.ADDRESS, "Burlington, MA");
        expected.setField(StandardField.AUTHOR, "West, Matthew");
        expected.setField(StandardField.ISBN, "0123751063 (pbk.)");
        expected.setField(new UnknownField("issuance"), "monographic");
        expected.setField(StandardField.KEYWORDS, "Database design, Data structures (Computer science)");
        expected.setField(StandardField.LANGUAGE, "eng");
        expected.setField(new UnknownField("lccn"), "2010045158");
        expected.setField(StandardField.NOTE, "Matthew West., Includes index.");
        expected.setField(new UnknownField("oclc"), "ocn665135773");
        expected.setField(StandardField.PUBLISHER, "Morgan Kaufmann");
        expected.setField(new UnknownField("source"), "DLC");
        expected.setField(StandardField.TITLE, "Developing high quality data models");
        expected.setField(StandardField.YEAR, "2011");

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
