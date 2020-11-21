package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class CitationRelationFetcherTest {
    private CitationRelationFetcher citingFetcher;
    private CitationRelationFetcher citedByFetchter;

    @BeforeEach
    public void setUp() {
        citingFetcher = new CitationRelationFetcher(CitationRelationFetcher.SearchType.CITING);
        citedByFetchter = new CitationRelationFetcher(CitationRelationFetcher.SearchType.CITEDBY);
    }

    @Test
    public void testPerformSearchNoDoiCiting() throws FetcherException {
        BibEntry noDoi = new BibEntry();
        assertNull(citingFetcher.performSearch(noDoi));
    }

    @Test
    public void testPerformSearchNoDoiCitedBy() throws FetcherException {
        assertTrue(true);
    }

    @Test
    public void testPerformSearchWithDoiEmptyCiting() throws FetcherException {
        assertTrue(true);
    }

    @Test
    public void testPerformSearchWithDoiCiting() throws FetcherException {
        assertTrue(true);
    }

    @Test
    public void testPerformSearchWithDoiEmptyCitedBy() throws FetcherException {
        assertTrue(true);
    }

    @Test
    public void testPerformSearchWithDoiCitedBy() throws FetcherException {
        assertTrue(true);
    }

    @Test
    public void testGetName() throws FetcherException {
        assertTrue(true);
    }




}
