package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
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
        bibEntry = new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.TITLE, "Query expansion using associated queries")
                .withField(StandardField.AUTHOR, "Billerbeck, Bodo and Scholer, Falk and Williams, Hugh E. and Zobel, Justin")
                .withField(StandardField.VOLUME, "0")
                .withField(StandardField.DOI, "10.1145/956863.956866")
                .withField(StandardField.JOURNAL, "Proceedings of the twelfth international conference on Information and knowledge management - CIKM '03")
                .withField(StandardField.PUBLISHER, "ACM Press")
                .withField(StandardField.TYPE, "paper-conference")
                .withField(StandardField.YEAR, "2003")
                .withField(StandardField.URL, "https://web.archive.org/web/20170810164449/http://goanna.cs.rmit.edu.au/~jz/fulltext/cikm03.pdf");
    }

    @Test
    public void getNameReturnsCorrectName() {
        assertEquals("ScholarArchive", fetcher.getName());
    }

    @Test
    public void performSearchReturnsExpectedResults() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("query");
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertTrue(fetchedEntries.contains(bibEntry), "Found the following entries " + fetchedEntries);
    }
}



