package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.testutils.category.FetcherTests;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category(FetcherTests.class)
public class MrDLibFetcherTest {

    private MrDLibFetcher fetcher;
    private BibEntry bibEntry;

    @Before
    public void setUp() {
        fetcher = new MrDLibFetcher("", "");
    }

    @Test
    public void testPerformSearch() throws FetcherException {
        bibEntry = new BibEntry();
        bibEntry.setField(FieldName.TITLE, "lernen");
        List<BibEntry> bibEntrys = fetcher.performSearch(bibEntry);
        assertFalse(bibEntrys.isEmpty());
    }

    @Test
    public void testPerformSearchForHornecker2006() throws FetcherException {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setCiteKey("Hornecker:2006:GGT:1124772.1124838");
        bibEntry.setField(FieldName.ADDRESS, "New York, NY, USA");
        bibEntry.setField(FieldName.AUTHOR, "Hornecker, Eva and Buur, Jacob");
        bibEntry.setField(FieldName.BOOKTITLE, "Proceedings of the SIGCHI Conference on Human Factors in Computing Systems");
        bibEntry.setField(FieldName.DOI, "10.1145/1124772.1124838");
        bibEntry.setField(FieldName.ISBN, "1-59593-372-7");
        bibEntry.setField(FieldName.KEYWORDS, "CSCW,analysis,collaboration,design,framework,social interaction,tangible interaction,tangible interface");
        bibEntry.setField(FieldName.PAGES, "437--446");
        bibEntry.setField(FieldName.PUBLISHER, "ACM");
        bibEntry.setField(FieldName.SERIES, "CHI '06");
        bibEntry.setField(FieldName.TITLE, "{Getting a Grip on Tangible Interaction: A Framework on Physical Space and Social Interaction}");
        bibEntry.setField(FieldName.URL, "http://doi.acm.org/10.1145/1124772.1124838");
        bibEntry.setField(FieldName.YEAR, "2006");

        List<BibEntry> bibEntrys = fetcher.performSearch(bibEntry);
        assertFalse(bibEntrys.isEmpty());
    }

    @Test
    public void testGetName() {
        assertEquals("MDL_FETCHER", fetcher.getName());
    }

}
