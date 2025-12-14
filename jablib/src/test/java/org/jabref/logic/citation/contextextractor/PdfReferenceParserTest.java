package org.jabref.logic.citation.contextextractor;

import java.util.List;

import org.jabref.model.citation.ReferenceEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfReferenceParserTest {

    private PdfReferenceParser parser;

    @BeforeEach
    void setUp() {
        parser = new PdfReferenceParser();
    }

    @Test
    void parseNumericBracketedReferences() {
        String text = """
                [1] Smith, J. (2020). Machine Learning Basics. Journal of AI, 10, 1-15.

                [2] Jones, A. and Brown, B. (2019). Deep Learning Applications. Nature, 500, 100-120.

                [3] Williams, C. et al. (2021). Neural Networks Today. Science, 350, 50-75.
                """;

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertEquals(3, references.size());
        assertEquals("[1]", references.getFirst().marker());
        assertEquals("[2]", references.get(1).marker());
        assertEquals("[3]", references.get(2).marker());
    }

    @Test
    void parseNumericDottedReferences() {
        String text = """
                1. Smith, J. (2020). First Paper Title. Journal A, 10, 1-15.

                2. Jones, A. (2019). Second Paper Title. Journal B, 20, 100-120.
                """;

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertEquals(2, references.size());
        assertEquals("[1]", references.getFirst().marker());
        assertEquals("[2]", references.get(1).marker());
    }

    @Test
    void parseAuthorKeyReferences() {
        String text = "[Smith20] Smith, J. (2020). Machine Learning Basics. Journal of AI, 10, 1-15.";

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertFalse(references.isEmpty());
        assertEquals("[Smith20]", references.getFirst().marker());
    }

    @Test
    void extractAuthors() {
        String text = "[1] Smith, J. and Jones, A. (2020). Test Paper. Journal, 10, 1-5.";

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertFalse(references.isEmpty());
        assertTrue(references.getFirst().authors().isPresent());
        assertTrue(references.getFirst().authors().get().contains("Smith"));
    }

    @Test
    void extractYear() {
        String text = "[1] Smith, J. (2020). Test Paper Title. Journal of Testing, 10, 1-15.";

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertFalse(references.isEmpty());
        assertEquals("2020", references.getFirst().year().orElse(""));
    }

    @Test
    void extractDoi() {
        String text = "[1] Smith, J. (2020). Test Paper. Journal, 10, 1-5. doi:10.1234/example.2020";

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertFalse(references.isEmpty());
        assertTrue(references.getFirst().doi().isPresent());
        assertTrue(references.getFirst().doi().get().contains("10.1234"));
    }

    @Test
    void extractUrl() {
        String text = "[1] Smith, J. (2020). Online Resource. Available at: https://example.com/paper";

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertFalse(references.isEmpty());
        assertTrue(references.getFirst().url().isPresent());
        assertEquals("https://example.com/paper", references.getFirst().url().get());
    }

    @Test
    void extractJournal() {
        String text = "[1] Smith, J. (2020). Test Paper. Nature Reviews Machine Learning, 10, 1-15.";

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertFalse(references.isEmpty());
    }

    @Test
    void extractVolumeAndPages() {
        String text = "[1] Smith, J. Test Paper. Journal 2020; 55: 100-115.";

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertFalse(references.isEmpty());
        ReferenceEntry ref = references.getFirst();
        assertFalse(ref.rawText().isEmpty());
    }

    @Test
    void emptyTextReturnsEmptyList() {
        List<ReferenceEntry> references = parser.parseReferences("");
        assertTrue(references.isEmpty());
    }

    @Test
    void nullTextReturnsEmptyList() {
        List<ReferenceEntry> references = parser.parseReferences(null);
        assertTrue(references.isEmpty());
    }

    @Test
    void shortTextFiltered() {
        String text = "[1] Short";
        List<ReferenceEntry> references = parser.parseReferences(text);
        assertTrue(references.isEmpty());
    }

    @Test
    void multiLineReference() {
        String text = """
                [1] Smith, J., Jones, A., Williams, B., Brown, C., Davis, D.,
                    Miller, E., and Wilson, F. (2020). A Very Long Paper Title
                    That Spans Multiple Lines. Journal of Extended Research,
                    100(5), 1000-1050.

                [2] Another, A. (2019). Short Paper. Journal, 1, 1-5.
                """;

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertTrue(references.size() >= 2);
        assertTrue(references.stream().anyMatch(r -> r.marker().equals("[1]")));
        assertTrue(references.stream().anyMatch(r -> r.marker().equals("[2]")));
    }

    @Test
    void authorYearFormatGeneration() {
        String text = """
                Smith, J. (2020). First Paper Title. Journal A, 10, 1-15.

                Jones, A. (2019). Second Paper Title. Journal B, 20, 100-120.
                """;

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertFalse(references.isEmpty());
        assertTrue(references.stream()
                             .anyMatch(r -> r.marker().contains("Smith") || r.marker().contains("2020")));
    }

    @Test
    void extractQuotedTitle() {
        String text = "[1] Smith, J. (2020). \"This is a Quoted Title\". Journal, 10, 1-5.";

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertFalse(references.isEmpty());
        assertTrue(references.getFirst().title().isPresent());
        assertEquals("This is a Quoted Title", references.getFirst().title().get());
    }

    @Test
    void mixedReferenceFormats() {
        String text = """
                [1] Smith J. Machine Learning Today. Nature 2020;500:100-110.

                [2] Jones, A., & Brown, B. (2019). Deep Learning. Science, 350, 50-75. https://doi.org/10.1234/science

                [3] Williams C et al. Neural Networks. J AI Res 2021; 10: 1-15
                """;

        List<ReferenceEntry> references = parser.parseReferences(text);

        assertEquals(3, references.size());
    }
}
