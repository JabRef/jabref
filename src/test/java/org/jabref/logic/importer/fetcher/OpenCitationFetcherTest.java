package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@FetcherTest
public class OpenCitationFetcherTest {
    private OpenCitationFetcher openCitationFetcher;

    @BeforeEach
    public void setUp() {
        openCitationFetcher = new OpenCitationFetcher();
    }

    @Test
    public void testPerformSearchNoDoiCiting() throws FetcherException {
        BibEntry noDoi = new BibEntry();
        assertThrows(FetcherException.class, () -> openCitationFetcher.searchCiting(noDoi));
    }

    @Test
    public void testPerformSearchNoDoiCitedBy() throws FetcherException {
        BibEntry noDoi = new BibEntry();
        assertThrows(FetcherException.class, () -> openCitationFetcher.searchCitedBy(noDoi));
    }

    @Test
    public void testPerformSearchWithDoiEmptyCiting() throws FetcherException {
        BibEntry doiEmptyCiting = new BibEntry();
        doiEmptyCiting.setField(StandardField.DOI, "10.1.1.19.4684");
        assertTrue(openCitationFetcher.searchCiting(doiEmptyCiting).isEmpty());
    }

    @Test
    public void testPerformSearchWithDoiCiting() throws FetcherException {
        BibEntry doiCiting = new BibEntry();
        doiCiting.setField(StandardField.DOI, "10.1109/TCBB.2011.83");
        assertFalse(openCitationFetcher.searchCiting(doiCiting).isEmpty());
    }

    @Test
    public void testPerformSearchWithDoiEmptyCitedBy() throws FetcherException {
        BibEntry doiEmptyCitedBy = new BibEntry();
        doiEmptyCitedBy.setField(StandardField.DOI, "10.1.1.19.4684");
        assertTrue(openCitationFetcher.searchCitedBy(doiEmptyCitedBy).isEmpty());
    }

    @Test
    public void testPerformSearchWithDoiCitedBy() throws FetcherException {
        BibEntry doiCitedBy = new BibEntry();
        doiCitedBy.setField(StandardField.DOI, "10.1109/TCBB.2011.83");
        assertFalse(openCitationFetcher.searchCitedBy(doiCitedBy).isEmpty());
    }

    @Test
    public void testGetName() throws FetcherException {
        assertEquals(openCitationFetcher.getName(), "CitationRelationFetcher");
    }
}
