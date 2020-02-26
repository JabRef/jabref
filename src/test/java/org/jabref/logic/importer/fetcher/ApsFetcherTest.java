package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class ApsFetcherTest {

    private ApsFetcher finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        finder = new ApsFetcher();
        entry = new BibEntry();
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findOpenAccess() throws IOException {
        entry.setField(StandardField.DOI, "10.1103/PhysRevLett.116.061102");

        assertEquals(
                Optional.of(new URL("https://journals.aps.org/prl/pdf/10.1103/PhysRevLett.116.061102")),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void caseInsensitive() throws IOException {
        entry.setField(StandardField.DOI, "10.1103/physrevlett.124.029002");

        assertEquals(
                Optional.of(new URL("https://journals.aps.org/prl/pdf/10.1103/PhysRevLett.124.029002")),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void notAuthorized() throws IOException {
        entry.setField(StandardField.DOI, "10.1103/PhysRevLett.89.127401");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void notFoundByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.aasri.2014.0559.002");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
