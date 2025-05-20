package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.util.Optional;

import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
@Disabled("See https://github.com/JabRef/jabref/issues/12864")
class ApsFetcherTest {

    private ApsFetcher finder;

    @BeforeEach
    void setUp() {
        finder = new ApsFetcher();
    }

    @Test
    void findFullTextFromDoi() throws IOException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1103/PhysRevLett.116.061102");
        assertEquals(Optional.of(URLUtil.create("https://journals.aps.org/prl/pdf/10.1103/PhysRevLett.116.061102")), finder.findFullText(entry));
    }

    @Test
    void findFullTextFromLowercaseDoi() throws IOException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1103/physrevlett.124.029002");
        assertEquals(Optional.of(URLUtil.create("https://journals.aps.org/prl/pdf/10.1103/PhysRevLett.124.029002")), finder.findFullText(entry));
    }

    @Test
    void notFindFullTextForUnauthorized() throws IOException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1103/PhysRevLett.89.127401");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void notFindFullTextForUnknownEntry() throws IOException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1016/j.aasri.2014.0559.002");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
