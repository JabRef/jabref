package org.jabref.logic.importer.fetcher;

import java.util.Collections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@FetcherTest
public class OpenCitationFetcherTest {
    private OpenCitationFetcher openCitationFetcher;

    @BeforeEach
    public void setUp() {
        openCitationFetcher = new OpenCitationFetcher();
    }

    @Test
    public void performSearchNoDoiCitingMode() throws FetcherException {
        assertEquals(Collections.emptyList(), openCitationFetcher.searchCiting(new BibEntry()));
    }

    @Test
    public void performSearchNoDoiCitedByMode() throws FetcherException {
        assertEquals(Collections.emptyList(), openCitationFetcher.searchCitedBy(new BibEntry()));
    }

    @Test
    public void performSearchWithDoiEmptyCitingMode() throws FetcherException {
        BibEntry doiEmptyCiting = new BibEntry().withField(StandardField.DOI, "10.1.1.19.4684");
        assertEquals(Collections.emptyList(), openCitationFetcher.searchCitedBy(doiEmptyCiting));
    }

    @Test
    public void performSearchWithDoiCitingMode() throws FetcherException {
        BibEntry doiCiting = new BibEntry().withField(StandardField.DOI, "10.1109/pesc.1988.18187");
        assertFalse(openCitationFetcher.searchCiting(doiCiting).isEmpty());
    }

    @Test
    public void performSearchWithDoiEmptyCitedByMode() throws FetcherException {
        BibEntry doiEmptyCitedBy = new BibEntry().withField(StandardField.DOI, "10.1.1.19.4684");
        assertEquals(Collections.emptyList(), openCitationFetcher.searchCitedBy(doiEmptyCitedBy));
    }

    @Test
    public void performSearchWithDoiCitedByMode() throws FetcherException {
        BibEntry doiCitedBy = new BibEntry().withField(StandardField.DOI, "10.1109/pesc.1988.18187");
        assertFalse(openCitationFetcher.searchCitedBy(doiCitedBy).isEmpty());
    }

    @Test
    public void getName() throws FetcherException {
        assertEquals(openCitationFetcher.getName(), "OpenCitationFetcher");
    }

}
