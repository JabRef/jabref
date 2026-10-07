package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.util.IsbnExtractor;
import org.jabref.logic.util.PdfUtils;
import org.jabref.model.entry.identifier.ISBN;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PdfBookMetadataImportTest {

    @Test
    void extractsIsbnFromFrontBibliographicPage() throws IOException, URISyntaxException {
        assertExtraction("front-imprint.pdf", 2, List.of("9789660800007"));
    }

    @Test
    void extractsIsbnFromBackBibliographicPage() throws IOException, URISyntaxException {
        assertExtraction("back-imprint.pdf", 9, List.of("9789660800007"));
    }

    @Test
    void returnsNoIsbnWhenBookHasNoIsbn() throws IOException, URISyntaxException {
        assertExtraction("imprint-without-isbn.pdf", 2, List.of());
    }

    @Test
    void rejectsInvalidIsbn() throws IOException, URISyntaxException {
        assertExtraction("imprint-invalid-isbn.pdf", 2, List.of());
    }

    @Test
    void ignoresIsbnOutsideBibliographicPage() throws IOException, URISyntaxException {
        assertExtraction("imprint-with-distractor.pdf", 2, List.of("9789660800007"));
    }

    @Test
    void extractsMultipleIsbnsFromBibliographicPage() throws IOException, URISyntaxException {
        assertExtraction("imprint-multiple-isbns.pdf", 2,
                List.of("9780596520687", "9780134685991", "0596520689"));
    }

    @Test
    void findsNoBibliographicPageWhenOnlyOrdinaryTextContainsIsbn() throws IOException, URISyntaxException {
        try (PDDocument document = loadFixture("ordinary-text-isbn.pdf")) {
            List<String> candidatePages = PdfPageScanner.scanPages(document);

            assertEquals(Optional.empty(), BibliographicPageDetector.findBibliographicPage(candidatePages));
        }
    }

    private void assertExtraction(String fileName, int expectedPage, List<String> expectedIsbns)
            throws IOException, URISyntaxException {
        try (PDDocument document = loadFixture(fileName)) {
            List<String> candidatePages = PdfPageScanner.scanPages(document);
            Optional<String> bibliographicPage = BibliographicPageDetector.findBibliographicPage(candidatePages);

            assertEquals(Optional.of(PdfUtils.getPageContents(document, expectedPage)), bibliographicPage,
                    "The selected text must belong to the fixture's known physical imprint page");

            List<String> actualIsbns = new IsbnExtractor().extract(bibliographicPage.orElseThrow()).stream()
                                                          .map(ISBN::asString)
                                                          .toList();
            assertEquals(expectedIsbns, actualIsbns);
        }
    }

    private PDDocument loadFixture(String fileName) throws IOException, URISyntaxException {
        var resource = PdfBookMetadataImportTest.class.getResource("/pdfs/PdfBookMetadataImport/" + fileName);
        assertNotNull(resource, "Missing PDF fixture: " + fileName);
        return Loader.loadPDF(Path.of(resource.toURI()).toFile());
    }
}
