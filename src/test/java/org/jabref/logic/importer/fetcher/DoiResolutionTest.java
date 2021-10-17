package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class DoiResolutionTest {

    private DoiResolution finder;
    private BibEntry entry;

    @BeforeEach
    void setup() {
        DOIPreferences doiPreferences = mock(DOIPreferences.class);
        when(doiPreferences.isUseCustom()).thenReturn(false);
        finder = new DoiResolution(doiPreferences);
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
        assertEquals(Optional.of(new URL("https://onlinelibrary.wiley.com/doi/pdf/10.1002/acr2.11101")), finder.findFullText(entry));
    }

    @Test
    void citationMetaTagLeadsToFulltext() throws IOException {
        entry.setField(StandardField.DOI, "10.1007/978-3-319-89963-3_28");
        assertEquals(Optional.of(new URL("https://link.springer.com/content/pdf/10.1007%2F978-3-319-89963-3_28.pdf")), finder.findFullText(entry));
    }

    @Test
    void notReturnAnythingWhenMultipleLinksAreFound() throws IOException {
        entry.setField(StandardField.DOI, "10.1109/JXCDC.2019.2911135");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void returnAnythingWhenBehindSpringerPayWall() throws IOException {
        // Springer returns a HTML page instead of an empty page,
        // even if the user does not have access
        // We cannot easily handle this case, because other publisher return the wrong media type.
        entry.setField(StandardField.DOI, "10.1007/978-3-319-62594-2_12");
        assertEquals(Optional.of(new URL("https://link.springer.com/content/pdf/10.1007%2F978-3-319-62594-2_12.pdf")), finder.findFullText(entry));
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
