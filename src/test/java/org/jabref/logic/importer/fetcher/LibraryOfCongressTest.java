package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LibraryOfCongressTest {

    private LibraryOfCongress fetcher = new LibraryOfCongress();

    @Test
    public void performSearchById() throws Exception {
        BibEntry expected = new BibEntry();
        expected.setField("address", "Burlington, MA");
        expected.setField("author", "West, Matthew");
        expected.setField("isbn", "0123751063 (pbk.)");
        expected.setField("issuance", "monographic");
        expected.setField("keywords", "Database design, Data structures (Computer science)");
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
