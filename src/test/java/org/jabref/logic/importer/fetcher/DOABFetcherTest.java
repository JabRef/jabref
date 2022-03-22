package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class DOABFetcherTest {
    private DOABFetcher fetcher;
    private BibEntry David_Opal;
    private BibEntry Ronald_Snijder;

    @BeforeEach
    public void setUp() throws Exception {
        fetcher = new DOABFetcher();

        David_Opal = new BibEntry();
        David_Opal.setField(StandardField.AUTHOR, "Pol, David");
        David_Opal.setField(StandardField.TITLE, "I Open Fire");
        David_Opal.setField(StandardField.TYPE, "book");
        David_Opal.setField(StandardField.DOI, "10.21983/P3.0086.1.00");
        David_Opal.setField(StandardField.PAGES, "56");
        David_Opal.setField(StandardField.YEAR, "2014");

        Ronald_Snijder = new BibEntry();
        Ronald_Snijder.setField(StandardField.AUTHOR, "Snijder, Ronald");
        Ronald_Snijder.setField(StandardField.TITLE, "The deliverance of open access books");
        Ronald_Snijder.setField(StandardField.TYPE, "book");
        Ronald_Snijder.setField(StandardField.DOI, "10.26530/OAPEN_1004809");
        Ronald_Snijder.setField(StandardField.PAGES, "234");
        Ronald_Snijder.setField(StandardField.YEAR, "2019");
    }

    @Test
    public void TestGetName() {
        assertEquals("DOAB", fetcher.getName());
    }

    @Test
    public void TestPerformSearch() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("i open fire");
        assertFalse(entries.isEmpty());
        assertTrue(entries.contains(David_Opal));
    }

     @Test
    public void TestPerformSearch2() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("the deliverance of open access books");
        assertFalse(entries.isEmpty());
        assertTrue(entries.contains(Ronald_Snijder));
    }
}
