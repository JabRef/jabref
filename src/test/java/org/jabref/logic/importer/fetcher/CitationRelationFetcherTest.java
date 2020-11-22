package org.jabref.logic.importer.fetcher;

import org.jabref.gui.actions.StandardActions;
import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@FetcherTest
public class CitationRelationFetcherTest {
    private CitationRelationFetcher citingFetcher;
    private CitationRelationFetcher citedByFetcher;

    @BeforeEach
    public void setUp() {
        citingFetcher = new CitationRelationFetcher(CitationRelationFetcher.SearchType.CITING);
        citedByFetcher = new CitationRelationFetcher(CitationRelationFetcher.SearchType.CITEDBY);
    }

    @Test
    public void testPerformSearchNoDoiCiting() throws FetcherException {
        BibEntry noDoi = new BibEntry();
        assertNull(citingFetcher.performSearch(noDoi));
    }

    @Test
    public void testPerformSearchNoDoiCitedBy() throws FetcherException {
        BibEntry noDoi = new BibEntry();
        assertNull(citedByFetcher.performSearch(noDoi));
    }

    @Test
    public void testPerformSearchWithDoiEmptyCiting() throws FetcherException {
        BibEntry DoiEmptyCiting = new BibEntry();
        DoiEmptyCiting.setField(StandardField.DOI, "10.1.1.19.4684");
        assertTrue(citingFetcher.performSearch(DoiEmptyCiting).isEmpty());
    }

    @Test
    public void testPerformSearchWithDoiCiting() throws FetcherException {
        BibEntry DoiCiting = new BibEntry();
        DoiCiting.setField(StandardField.DOI, "10.1017/s0074180900169669");
        assertFalse(citingFetcher.performSearch(DoiCiting).isEmpty());
    }

    @Test
    public void testPerformSearchWithDoiEmptyCitedBy() throws FetcherException {
        BibEntry DoiEmptyCitedBy = new BibEntry();
        DoiEmptyCitedBy.setField(StandardField.DOI, "10.1.1.19.4684");
        assertTrue(citedByFetcher.performSearch(DoiEmptyCitedBy).isEmpty());
    }

    @Test
    public void testPerformSearchWithDoiCitedBy() throws FetcherException {
        BibEntry DoiCitedBy = new BibEntry();
        DoiCitedBy.setField(StandardField.DOI, "10.1017/s0074180900169669");
        assertFalse(citedByFetcher.performSearch(DoiCitedBy).isEmpty());
    }

    @Test
    public void testGetName() throws FetcherException {
        assertTrue(citedByFetcher.getName().equals("CitationRelationFetcher"));
    }




}
