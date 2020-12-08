package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class OpenCitationFetcherTest {
    private OpenCitationFetcher openCitationFetcher;

    @BeforeEach
    public void setUp() {
        openCitationFetcher = new OpenCitationFetcher();
    }

    @Test
    public void performSearchNoDoiCitingMode() throws FetcherException {
        assertThrows(FetcherException.class, () -> openCitationFetcher.searchCiting(new BibEntry()));
    }

    @Test
    public void performSearchNoDoiCitedByMode() throws FetcherException {
        assertThrows(FetcherException.class, () -> openCitationFetcher.searchCitedBy(new BibEntry()));
    }

    @Test
    public void performSearchWithDoiEmptyCitingMode() throws FetcherException {
        BibEntry doiEmptyCiting = new BibEntry().withField(StandardField.DOI, "10.1.1.19.4684");
        assertTrue(openCitationFetcher.searchCiting(doiEmptyCiting).isEmpty());
    }

    @Test
    public void performSearchWithDoiCitingMode() throws FetcherException {
        BibEntry doiCiting = new BibEntry().withField(StandardField.DOI, "10.1109/TCBB.2011.83");
        assertFalse(openCitationFetcher.searchCiting(doiCiting).isEmpty());
    }

    @Test
    public void performSearchWithDoiEmptyCitedByMode() throws FetcherException {
        BibEntry doiEmptyCitedBy = new BibEntry().withField(StandardField.DOI, "10.1.1.19.4684");
        assertTrue(openCitationFetcher.searchCitedBy(doiEmptyCitedBy).isEmpty());
    }

    @Test
    public void performSearchWithDoiCitedByMode() throws FetcherException {
        BibEntry doiCitedBy = new BibEntry().withField(StandardField.DOI, "10.1109/TCBB.2011.83");
        assertFalse(openCitationFetcher.searchCitedBy(doiCitedBy).isEmpty());
    }

    @Test
    public void getName() throws FetcherException {
        assertEquals(openCitationFetcher.getName(), "OpenCitationFetcher");
    }

}
