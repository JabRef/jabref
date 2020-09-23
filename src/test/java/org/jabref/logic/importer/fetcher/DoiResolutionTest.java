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
class DoiResolutionTest {

    private DoiResolution finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        finder = new DoiResolution();
        entry = new BibEntry();
    }

    @Test
    void linkWithPdfInTitleTag() throws IOException {
        entry.setField(StandardField.DOI, "10.1051/0004-6361/201527330");

        assertEquals(
                Optional.of(new URL("https://www.aanda.org/articles/aa/pdf/2016/01/aa27330-15.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    void linkWithPdfStringLeadsToFulltext() throws IOException {
        entry.setField(StandardField.DOI, "10.1002/acr2.11101");
        assertEquals(Optional.of(new URL("https://onlinelibrary.wiley.com/doi/epdf/10.1002/acr2.11101")), finder.findFullText(entry));
    }

    @Test
    void multipleLinksWithSmallEditDistanceLeadToFulltext() throws IOException {
        entry.setField(StandardField.DOI, "10.1002/acr2.11101");
        assertEquals(Optional.of(new URL("https://onlinelibrary.wiley.com/doi/epdf/10.1002/acr2.11101")), finder.findFullText(entry));
    }

    @Test
    void notReturnAnythingWhenMultipleLinksAreFound() throws IOException {
        entry.setField(StandardField.DOI, "10.1109/JXCDC.2019.2911135");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void notReturnAnythingWhenDOILeadsToSpringerLink() throws IOException {
        entry.setField(StandardField.DOI, "https://doi.org/10.1007/978-3-319-89963-3_28");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void notReturnAnythingWhenDOILeadsToIEEE() throws IOException {
        entry.setField(StandardField.DOI, "https://doi.org/10.1109/TTS.2020.2992669");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void notFoundByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1186/unknown-doi");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void entityWithoutDoi() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.SOURCE, finder.getTrustLevel());
    }
}
