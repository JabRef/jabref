package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.util.PdfUtils;
import org.jabref.model.entry.identifier.ISBN;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@NullMarked
class PdfBookMetadataExtractionTest {

    @ParameterizedTest
    @CsvSource({
            "front-citation.pdf, 2",
            "back-citation.pdf, 9"
    })
    void selectsPhysicalBibliographicPage(String fileName, int expectedPage) throws IOException, URISyntaxException {
        try (PDDocument document = loadFixture(fileName)) {
            assertEquals(Optional.of(PdfUtils.getPageContents(document, expectedPage)),
                    BibliographicPageDetector.findBibliographicPage(PdfPageScanner.scanPages(document)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "front-citation.pdf",
            "back-citation.pdf",
            "citation-without-isbn.pdf",
            "citation-invalid-isbn.pdf",
            "citation-equivalent-isbns.pdf",
            "citation-ambiguous-isbns.pdf",
            "citation-valid-and-invalid-isbns.pdf",
            "citation-competing-dates.pdf"
    })
        // [utest->req~import.pdf.book-citation-publication~1]
    void extractsCompleteCitationIndependentlyOfIsbnSelection(String fileName) throws IOException, URISyntaxException {
        String citation = BookCitationBlock.find(bibliographicPage(fileName)).orElseThrow();

        assertEquals("""
                Коваль О. П.
                К56 Збірник задач з фізики для 8 класу / О. П. Коваль,
                Н. І. Ткач. — 2-ге вид., перероб. — Харків : Ранок, 2010. — 176 с.
                """.strip(), citation);
        assertEquals(Optional.of("Збірник задач з фізики для 8 класу"), BookTitleAuthorExtractor.findTitle(citation));
        assertEquals(Optional.of("О. П. Коваль, Н. І. Ткач"), BookTitleAuthorExtractor.findAuthors(citation));
        assertEquals(Optional.empty(), BookTitleAuthorExtractor.findEditors(citation));
        assertEquals(Optional.of("Ранок"), BookCitationBlock.findPublisher(citation));
        assertEquals(Optional.of("2010"), BookCitationBlock.findYear(citation));
        assertEquals(Optional.of("2"), BookCitationBlock.findEdition(citation));
    }

    @Test
    void leavesMissingOptionalFieldsEmpty() throws IOException, URISyntaxException {
        String citation = BookCitationBlock.find(bibliographicPage("citation-missing-optional-fields.pdf")).orElseThrow();

        assertEquals(Optional.of("Збірник задач з фізики для 8 класу"), BookTitleAuthorExtractor.findTitle(citation));
        assertEquals(Optional.of("О. П. Коваль, Н. І. Ткач"), BookTitleAuthorExtractor.findAuthors(citation));
        assertEquals(Optional.of("Ранок"), BookCitationBlock.findPublisher(citation));
        assertEquals(Optional.of("2010"), BookCitationBlock.findYear(citation));
        assertEquals(Optional.empty(), BookCitationBlock.findEdition(citation));
        assertEquals(Optional.empty(), BookTitleAuthorExtractor.findEditors(citation));
    }

    @Test
    void extractsLabelledEditorAndEditionWithoutInventingAuthors() throws IOException, URISyntaxException {
        String citation = BookCitationBlock.find(bibliographicPage("citation-editor-edition.pdf")).orElseThrow();

        assertEquals(Optional.of("Збірник задач"), BookTitleAuthorExtractor.findTitle(citation));
        assertEquals(Optional.empty(), BookTitleAuthorExtractor.findAuthors(citation));
        assertEquals(Optional.of("О. П. Коваля"), BookTitleAuthorExtractor.findEditors(citation));
        assertEquals(Optional.of("Ранок"), BookCitationBlock.findPublisher(citation));
        assertEquals(Optional.of("2010"), BookCitationBlock.findYear(citation));
        assertEquals(Optional.of("2"), BookCitationBlock.findEdition(citation));
    }

    @Test
        // [utest->req~import.pdf.book-citation-publication~1]
    void rejectsIncompleteCitationDespiteClassificationAndIsbn() throws IOException, URISyntaxException {
        String page = bibliographicPage("incomplete-citation.pdf");

        assertEquals(Optional.empty(), BookCitationBlock.find(page));
        assertEquals(Optional.empty(), BookCitationBlock.findPublisher(page));
        assertEquals(Optional.empty(), BookCitationBlock.findYear(page));
        assertEquals(Optional.of("9789660800007"), ISBN.findInText(page).map(ISBN::asString));
    }

    @Test
        // [utest->req~import.pdf.conference-article-exclusion~1]
    void excludesConferenceArticleDespiteValidProceedingsIsbn() throws IOException, URISyntaxException {
        try (PDDocument document = loadFixture("conference-paper.pdf")) {
            assertEquals(List.of("9780134685991"),
                    ISBN.findAllInText(PdfUtils.getPageContents(document, 1)).stream().map(ISBN::asString).toList());
            assertEquals(Optional.empty(), BibliographicPageDetector.findBibliographicPage(PdfPageScanner.scanPages(document)));
        }
    }

    @ParameterizedTest
    @MethodSource("isbnCases")
    void extractsAllValidCandidates(String fileName, List<String> expectedCandidates, Optional<String> expectedSelection)
            throws IOException, URISyntaxException {
        assertEquals(expectedCandidates,
                ISBN.findAllInText(bibliographicPage(fileName)).stream().map(ISBN::asString).toList());
    }

    @ParameterizedTest
    @MethodSource("isbnCases")
    void selectsOnlyUnambiguousIsbnIdentity(String fileName, List<String> expectedCandidates, Optional<String> expectedSelection)
            throws IOException, URISyntaxException {
        assertEquals(expectedSelection, ISBN.findInText(bibliographicPage(fileName)).map(ISBN::asString));
    }

    private static Stream<Arguments> isbnCases() {
        return Stream.of(
                Arguments.of("front-citation.pdf", List.of("9789660800007"), Optional.of("9789660800007")),
                Arguments.of("back-citation.pdf", List.of("9789660800007"), Optional.of("9789660800007")),
                Arguments.of("citation-without-isbn.pdf", List.of(), Optional.empty()),
                Arguments.of("citation-invalid-isbn.pdf", List.of(), Optional.empty()),
                Arguments.of("citation-equivalent-isbns.pdf", List.of("9780596520687", "0596520689"), Optional.of("9780596520687")),
                Arguments.of("citation-ambiguous-isbns.pdf", List.of("9780596520687", "9780134685991"), Optional.empty()),
                Arguments.of("citation-valid-and-invalid-isbns.pdf", List.of("9789660800007"), Optional.of("9789660800007")),
                Arguments.of("citation-competing-dates.pdf", List.of("9789660800007"), Optional.of("9789660800007")),
                Arguments.of("citation-missing-optional-fields.pdf", List.of("9789660800007"), Optional.of("9789660800007")),
                Arguments.of("citation-editor-edition.pdf", List.of("9789660800007"), Optional.of("9789660800007")));
    }

    private String bibliographicPage(String fileName) throws IOException, URISyntaxException {
        try (PDDocument document = loadFixture(fileName)) {
            return BibliographicPageDetector.findBibliographicPage(PdfPageScanner.scanPages(document)).orElseThrow();
        }
    }

    private PDDocument loadFixture(String fileName) throws IOException, URISyntaxException {
        var resource = getClass().getResource("/pdfs/PdfBookMetadataImport/" + fileName);
        assertNotNull(resource, "Missing PDF fixture: " + fileName);
        return Loader.loadPDF(Path.of(resource.toURI()).toFile());
    }
}
