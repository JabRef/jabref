package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class WillyFetcherTest {
    public static final String URL = "https://onlinelibrary.wiley.com/doi/pdf/";
    public static final String DOI = "10.1002/we.2952";
    public static final String INVALID_DOI = "10.1021/acsc.4c00971";

    WillyFetcher sut = new WillyFetcher();

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void shouldFindUrlByDoi() throws IOException, FetcherException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, DOI);

        Optional<URL> result = sut.findFullText(entry);

        assertEquals(Optional.of(URLUtil.create(URL + DOI)), result);
    }
//
//    @Test
//    @DisabledOnCIServer("CI server is unreliable")
//    void shouldHandleInvalidDoi() throws FetcherException, IOException {
//        BibEntry entry = new BibEntry().withField(StandardField.DOI, INVALID_DOI);
//        assertEquals(Optional.empty(), sut.findFullText(entry));
//    }

    @Test
    void shouldHandleEntityWithoutDoi() throws FetcherException, IOException {
        assertEquals(Optional.empty(), sut.findFullText(new BibEntry()));
    }

    @Test
    void shouldReturnCorrectTrustLevel() {
        assertEquals(TrustLevel.SOURCE, sut.getTrustLevel());
    }
}
