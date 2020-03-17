package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class ApsFetcherTest {

    private ApsFetcher finder;

    @BeforeEach
    void setUp() {
        finder = new ApsFetcher();
    }

    private static Stream<Arguments> provideBibEntriesWithDois() throws IOException {
        // Standard DOI works
        BibEntry easy = new BibEntry().withField(StandardField.DOI, "10.1103/PhysRevLett.116.061102");
        Optional<URL> pdfUrl1 = Optional.of(new URL("https://journals.aps.org/prl/pdf/10.1103/PhysRevLett.116.061102"));

        // DOI in lowercase works
        BibEntry lowercase = new BibEntry().withField(StandardField.DOI, "10.1103/physrevlett.124.029002");
        Optional<URL> pdfUrl2 = Optional.of(new URL("https://journals.aps.org/prl/pdf/10.1103/PhysRevLett.124.029002"));

        // Article behind paywall returns empty
        BibEntry unauthorized = new BibEntry().withField(StandardField.DOI, "10.1103/PhysRevLett.89.127401");

        // Unavailable article returns empty
        BibEntry notFoundByDoi = new BibEntry().withField(StandardField.DOI, "10.1016/j.aasri.2014.0559.002");

        return Stream.of(
                         Arguments.of(pdfUrl1, easy),
                         Arguments.of(pdfUrl2, lowercase),
                         Arguments.of(Optional.empty(), unauthorized),
                         Arguments.of(Optional.empty(), notFoundByDoi));
    }

    @ParameterizedTest
    @MethodSource("provideBibEntriesWithDois")
    void shouldReturnFullTextUrlOrEmpty(Optional<URL> expected, BibEntry entry) throws IOException {
        assertEquals(expected, finder.findFullText(entry));
    }
}
