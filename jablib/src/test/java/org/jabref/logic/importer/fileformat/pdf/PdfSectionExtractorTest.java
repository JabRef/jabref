package org.jabref.logic.importer.fileformat.pdf;

import java.util.List;
import java.util.Optional;

import org.jabref.model.pdf.PdfDocumentSections;
import org.jabref.model.pdf.PdfSection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfSectionExtractorTest {

    private PdfSectionExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfSectionExtractor();
    }

    @Test
    void pdfSectionCreation() {
        PdfSection section = new PdfSection("Related Work", "Some content about related work", 1, 2);
        assertEquals("Related Work", section.name());
        assertEquals("Some content about related work", section.content());
        assertEquals(1, section.startPage());
        assertEquals(2, section.endPage());
    }

    @Test
    void pdfSectionValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("", "content", 1, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", null, 1, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", "content", 0, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", "content", 2, 1));
    }

    @Test
    void pdfDocumentSectionsFindsCitationRelevantSections() {
        List<PdfSection> sections = List.of(
                new PdfSection("Introduction", "Intro content", 1, 2),
                new PdfSection("Related Work", "Related work content", 3, 5),
                new PdfSection("Methodology", "Method content", 6, 8),
                new PdfSection("Literature Review", "Review content", 9, 10)
        );

        PdfDocumentSections docSections = new PdfDocumentSections("full text", sections, 10);

        List<PdfSection> citationSections = docSections.getCitationRelevantSections();
        assertEquals(2, citationSections.size());
        assertTrue(citationSections.stream().anyMatch(s -> s.name().equals("Related Work")));
        assertTrue(citationSections.stream().anyMatch(s -> s.name().equals("Literature Review")));
    }

    @Test
    void pdfDocumentSectionsFindSection() {
        List<PdfSection> sections = List.of(
                new PdfSection("Introduction", "Intro content", 1, 2),
                new PdfSection("Related Work", "Related work content", 3, 5)
        );

        PdfDocumentSections docSections = new PdfDocumentSections("full text", sections, 5);

        Optional<PdfSection> found = docSections.findSection("related");
        assertTrue(found.isPresent());
        assertEquals("Related Work", found.get().name());

        Optional<PdfSection> notFound = docSections.findSection("conclusion");
        assertFalse(notFound.isPresent());
    }

    @Test
    void hasCitationRelevantSections() {
        List<PdfSection> withRelevant = List.of(
                new PdfSection("Background", "Content", 1, 2)
        );
        PdfDocumentSections docWithRelevant = new PdfDocumentSections("text", withRelevant, 2);
        assertTrue(docWithRelevant.hasCitationRelevantSections());

        List<PdfSection> withoutRelevant = List.of(
                new PdfSection("Methodology", "Content", 1, 2)
        );
        PdfDocumentSections docWithoutRelevant = new PdfDocumentSections("text", withoutRelevant, 2);
        assertFalse(docWithoutRelevant.hasCitationRelevantSections());
    }
}
