package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@FetcherTest
public class SpringerLinkTest {

    private SpringerLink finder;
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        finder = new SpringerLink();
        entry = new BibEntry();
    }

    @Test
    public void rejectNullParameter() {
        assertThrows(NullPointerException.class, () -> finder.findFullText(null));
    }

    @Test
    public void doiNotPresent() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void findByDOI() throws IOException {
        entry.setField("doi", "10.1186/s13677-015-0042-8");
        assertEquals(
                Optional.of(new URL("http://link.springer.com/content/pdf/10.1186/s13677-015-0042-8.pdf")),
                finder.findFullText(entry));
    }

    @Test
    public void notFoundByDOI() throws IOException {
        entry.setField("doi", "10.1186/unknown-doi");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
