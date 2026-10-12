package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.IsbnExtractor;
import org.jabref.logic.util.PdfUtils;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void importsBookFromFrontBibliographicPage() throws URISyntaxException {
        assertImportedBook("front-imprint.pdf", "9789660800007");
    }

    @Test
    void importsBookFromBackBibliographicPage() throws URISyntaxException {
        assertImportedBook("back-imprint.pdf", "9789660800007");
    }

    @Test
    void importsBookUsingBibliographicIsbnInsteadOfDistractor() throws URISyntaxException {
        assertImportedBook("imprint-with-distractor.pdf", "9789660800007");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "imprint-without-isbn.pdf",
            "imprint-invalid-isbn.pdf"
    })
    void fallsBackToPaperImportWhenBibliographicPageHasNoValidIsbn(String fileName) throws URISyntaxException {
        assertImportedPaper(fileName);
    }

    @Test
    void returnsNoEntriesWhenBibliographicPageHasMultipleValidIsbns() throws URISyntaxException {
        ParserResult result = importFixture("imprint-multiple-isbns.pdf");

        assertEquals(List.of(), result.getDatabase().getEntries());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void fallsBackToPaperImportWhenNoBibliographicPageIsDetected() throws URISyntaxException {
        assertImportedPaper("ordinary-text-isbn.pdf");
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

    private void assertImportedBook(String fileName, String expectedIsbn) throws URISyntaxException {
        BibEntry entry = importSingleEntry(fileName);

        assertEquals(StandardEntryType.Book, entry.getType(),
                "Expected a book entry for " + fileName);
        assertEquals(Optional.of(expectedIsbn), entry.getField(StandardField.ISBN),
                "Unexpected ISBN for " + fileName);
    }

    private void assertImportedPaper(String fileName) throws URISyntaxException {
        BibEntry entry = importSingleEntry(fileName);

        assertEquals(StandardEntryType.InProceedings, entry.getType());
        assertEquals(Optional.empty(), entry.getField(StandardField.ISBN));
        assertFalse(entry.getTitle().orElseThrow().isBlank(), "Paper fallback must extract a title");
    }

    private BibEntry importSingleEntry(String fileName) throws URISyntaxException {
        ParserResult result = importFixture(fileName);
        List<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals(1, entries.size(), "Expected one imported entry for " + fileName);

        return entries.getFirst();
    }

    private ParserResult importFixture(String fileName) throws URISyntaxException {
        var resource = PdfBookMetadataImportTest.class.getResource("/pdfs/PdfBookMetadataImport/" + fileName);
        assertNotNull(resource, "Missing PDF fixture: " + fileName);

        Path file = Path.of(resource.toURI());
        ParserResult result = new PdfContentImporter().importDatabase(file);

        assertFalse(result.isInvalid(),
                () -> "Import failed for " + fileName + ": " + result.getErrorMessage());

        return result;
    }
}
