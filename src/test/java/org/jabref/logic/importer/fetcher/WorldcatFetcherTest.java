package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class WorldcatFetcherTest {

    private WorldcatFetcher fetcher;

    @BeforeEach
    public void setUp() {
        fetcher = new WorldcatFetcher("aMHOf2rfzUt3fuKkb7DXX8pkBv1AmcBWwwoSfwpt8CMhdUdxXscB4ESOmBPs4NlmYJmFtcSZ3Q5kMxzb");
    }

    @Test
    public void testPerformSearchForBadTitle() throws FetcherException {
        BibEntry entry = new BibEntry();
        // Mashing keyboard. Verified on https://platform.worldcat.org/api-explorer/apis/wcapi/Bib/OpenSearch
        entry.setField(StandardField.TITLE, "ASDhbsdfnm");
        List<BibEntry> list = fetcher.performSearch(entry);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testPerformSearchForExistingTitle() throws FetcherException {
        BibEntry entry = new BibEntry();
        // Example "The very best of Glenn Miller". Verified on same link as above
        entry.setField(StandardField.TITLE, "The very best of Glenn");
        List<BibEntry> list = fetcher.performSearch(entry);
        assertFalse(list.isEmpty());
    }
}
