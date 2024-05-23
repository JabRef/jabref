package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class ScholarArchiveFetcherTest {
    private ScholarArchiveFetcher fetcher;
    private BibEntry bibEntry;

    @BeforeEach
    public void setUp() {
        fetcher = new ScholarArchiveFetcher();
        bibEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.TITLE, "BPELscript: A Simplified Script Syntax for WS-BPEL 2.0")
                .withField(StandardField.AUTHOR, "Marc Bischof and Oliver Kopp and Tammo van Lessen and Frank Leymann ")
                .withField(StandardField.YEAR, "2009");
    }

    @Test
    public void getNameReturnsCorrectName() {
        assertEquals("ScholarArchive", fetcher.getName());
    }

    @Test
    @Disabled("We seem to be blocked")
    public void performSearchReturnsExpectedResults() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("bpelscript");
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertTrue(fetchedEntries.contains(bibEntry), "Found the following entries " + fetchedEntries);
    }
}



