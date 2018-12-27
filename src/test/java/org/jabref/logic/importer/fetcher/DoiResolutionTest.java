package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class DoiResolutionTest {

    private DoiResolution finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        finder = new DoiResolution();
        entry = new BibEntry();
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDOI() throws IOException {
        entry.setField("doi", "10.1051/0004-6361/201527330");

        assertEquals(
                Optional.of(new URL("https://www.aanda.org/articles/aa/pdf/2016/01/aa27330-15.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    void notReturnAnythingWhenMultipleLinksAreFound() throws IOException {
        entry.setField("doi", "10.1051/0004-6361/201527330; 10.1051/0004-6361/20152711233");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void notFoundByDOI() throws IOException {
        entry.setField("doi", "10.1186/unknown-doi");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
