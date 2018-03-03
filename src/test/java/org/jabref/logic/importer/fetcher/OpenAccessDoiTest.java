package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
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
        entry.setField("doi", "10.1038/nature12373");

        assertEquals(
                Optional.of(new URL("https://dash.harvard.edu/bitstream/handle/1/12285462/Nanometer-Scale%20Thermometry.pdf?sequence=1")),
                finder.findFullText(entry)
        );
    }

    @Test
    void notFoundByDOI() throws IOException {
        entry.setField("doi", "10.1186/unknown-doi");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
