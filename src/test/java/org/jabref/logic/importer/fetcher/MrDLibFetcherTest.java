package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.util.Version;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.MrDlibPreferences;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@FetcherTest
public class MrDLibFetcherTest {

    private MrDLibFetcher fetcher;

    @BeforeEach
    public void setUp() {
        MrDlibPreferences mrDlibPreferences = new MrDlibPreferences(
                true,
                false,
                false,
                false);
        fetcher = new MrDLibFetcher("", Version.parse(""), mrDlibPreferences);
    }

    @Test
    public void testPerformSearch() throws FetcherException {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.TITLE, "lernen");
        List<BibEntry> bibEntrys = fetcher.performSearch(bibEntry);
        assertFalse(bibEntrys.isEmpty());
    }

    @Test
    public void testPerformSearchForHornecker2006() throws FetcherException {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setCitationKey("Hornecker:2006:GGT:1124772.1124838");
        bibEntry.setField(StandardField.ADDRESS, "New York, NY, USA");
        bibEntry.setField(StandardField.AUTHOR, "Hornecker, Eva and Buur, Jacob");
        bibEntry.setField(StandardField.BOOKTITLE, "Proceedings of the SIGCHI Conference on Human Factors in Computing Systems");
        bibEntry.setField(StandardField.DOI, "10.1145/1124772.1124838");
        bibEntry.setField(StandardField.ISBN, "1-59593-372-7");
        bibEntry.setField(StandardField.KEYWORDS, "CSCW,analysis,collaboration,design,framework,social interaction,tangible interaction,tangible interface");
        bibEntry.setField(StandardField.PAGES, "437--446");
        bibEntry.setField(StandardField.PUBLISHER, "ACM");
        bibEntry.setField(StandardField.SERIES, "CHI '06");
        bibEntry.setField(StandardField.TITLE, "{Getting a Grip on Tangible Interaction: A Framework on Physical Space and Social Interaction}");
        bibEntry.setField(StandardField.URL, "http://doi.acm.org/10.1145/1124772.1124838");
        bibEntry.setField(StandardField.YEAR, "2006");

        List<BibEntry> bibEntrys = fetcher.performSearch(bibEntry);
        assertFalse(bibEntrys.isEmpty());
    }

    @Test
    public void testGetName() {
        assertEquals("MDL_FETCHER", fetcher.getName());
    }
}
