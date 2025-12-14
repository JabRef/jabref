package org.jabref.logic.citation.contextextractor;

import org.jabref.model.citation.CitationContext;
import org.jabref.model.citation.CitationContextList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CitationContextExtractorTest {

    private CitationContextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new CitationContextExtractor();
    }

    @Test
    void extractAuthorYearCitation() {
        String text = "Colombia is a middle-income country with a population of approximately 50 million (CIA 2021), with at least 11 million people living in rural areas.";
        CitationContextList result = extractor.extractContexts(text, "LunaOstos_2024");

        assertFalse(result.isEmpty());
        assertTrue(result.getContexts().stream()
                         .anyMatch(ctx -> ctx.citationMarker().contains("CIA 2021")));
    }

    @Test
    void extractNumericCitation() {
        String text = "Previous research has shown significant results [1]. The methodology was later improved [2,3].";

        CitationContextList result = extractor.extractContexts(text, "TestPaper");

        assertFalse(result.isEmpty());
        assertTrue(result.getUniqueCitationMarkers().stream()
                         .anyMatch(m -> m.equals("[1]")));
    }

    @Test
    void extractInlineAuthorCitation() {
        String text = "Smith et al. (2020) demonstrated that the approach works well in practice.";

        CitationContextList result = extractor.extractContexts(text, "Source2024");

        assertFalse(result.isEmpty());
        assertTrue(result.getContexts().stream()
                         .anyMatch(ctx -> ctx.getNormalizedMarker().contains("Smith")));
    }

    @Test
    void extractMultipleCitations() {
        String text = """
                The field has seen significant advances (Jones 2019).
                Building on this work, Smith and Brown (2020) proposed a new framework.
                Recent studies [1,2] have validated these findings.
                """;

        CitationContextList result = extractor.extractContexts(text, "Review2024");

        assertTrue(result.size() >= 3);
    }

    @Test
    void emptyTextReturnsEmptyList() {
        CitationContextList result = extractor.extractContexts("", "Source");
        assertTrue(result.isEmpty());
    }

    @Test
    void textWithNoCitationReturnsEmptyList() {
        String text = "This is just regular text without any citations or references.";

        CitationContextList result = extractor.extractContexts(text, "Source");

        assertTrue(result.isEmpty());
    }

    @Test
    void contextIncludesSurroundingSentences() {
        String text = "First sentence provides background. The main finding (Author 2020) was significant. This has implications for future work.";

        CitationContextList result = extractor.extractContexts(text, "Source");

        assertFalse(result.isEmpty());
        CitationContext ctx = result.getContexts().getFirst();
        assertTrue(ctx.contextText().contains("main finding"));
    }

    @Test
    void findByMarker() {
        String text = "Study A (Smith 2020) and Study B (Jones 2021) both contributed.";

        CitationContextList result = extractor.extractContexts(text, "Source");

        assertFalse(result.findByMarker("Smith").isEmpty());
        assertFalse(result.findByMarker("Jones").isEmpty());
        assertTrue(result.findByMarker("Brown").isEmpty());
    }
}
