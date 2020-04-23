package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class OpenAccessDoiTest {

    private OpenAccessDoi finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        finder = new OpenAccessDoi();
        entry = new BibEntry();
    }

    @Test
    void findByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1038/nature12373");
        assertEquals(Optional.of(new URL("https://dash.harvard.edu/bitstream/1/12285462/1/Nanometer-Scale%20Thermometry.pdf")), finder.findFullText(entry));
    }

    @Test
    void notFoundByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1186/unknown-doi");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void entryWithoutDoi() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.META_SEARCH, finder.getTrustLevel());
    }
}
