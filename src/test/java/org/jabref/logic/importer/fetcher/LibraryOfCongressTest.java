package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class LibraryOfCongressTest {

    private final LibraryOfCongress fetcher = new LibraryOfCongress(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

    @Test
    public void performSearchById() throws Exception {
        BibEntry expected = new BibEntry();
        expected.setField("address", "Burlington, MA");
        expected.setField("author", "West, Matthew");
        expected.setField("isbn", "0123751063 (pbk.)");
        expected.setField("issuance", "monographic");
        expected.setField("keywords", "Database design Data structures (Computer science)");
        expected.setField("language", "eng");
        expected.setField("lccn", "2010045158");
        expected.setField("note", "Matthew West., Includes index.");
        expected.setField("oclc", "ocn665135773");
        expected.setField("publisher", "Morgan Kaufmann");
        expected.setField("source", "DLC");
        expected.setField("title", "Developing high quality data models");
        expected.setField("year", "2011");

        assertEquals(Optional.of(expected), fetcher.performSearchById("2010045158"));
    }
}
