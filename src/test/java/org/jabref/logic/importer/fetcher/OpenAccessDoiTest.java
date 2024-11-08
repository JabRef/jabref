package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URI;
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
        entry.setField(StandardField.DOI, "10.1186/s12993-024-00248-9");
        assertEquals(Optional.of(URI.create("https://behavioralandbrainfunctions.biomedcentral.com/counter/pdf/10.1186/s12993-024-00248-9").toURL()), finder.findFullText(entry));
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
