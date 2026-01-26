package org.jabref.model.citation;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CitationContextTest {

    @Test
    void basicCreation() {
        CitationContext context = new CitationContext(
                "(CIA 2021)",
                "Colombia is a middle-income country with a population of approximately 50 million.",
                "LunaOstos_2024"
        );

        assertEquals("(CIA 2021)", context.citationMarker());
        assertEquals("Colombia is a middle-income country with a population of approximately 50 million.", context.contextText());
        assertEquals("LunaOstos_2024", context.sourceCitationKey());
        assertEquals(Optional.empty(), context.pageNumber());
    }

    @Test
    void creationWithPageNumber() {
        CitationContext context = new CitationContext(
                "[1]",
                "Some context text",
                "Source2024",
                5
        );

        assertEquals(Optional.of(5), context.pageNumber());
    }

    @Test
    void formatForComment() {
        CitationContext context = new CitationContext(
                "(Smith 2020)",
                "This paper discusses important findings.",
                "MyPaper2024"
        );

        assertEquals("[MyPaper2024]: This paper discusses important findings.", context.formatForComment());
    }

    @Test
    void getNormalizedMarker() {
        CitationContext context1 = new CitationContext("(Smith 2020)", "text", "source");
        assertEquals("Smith 2020", context1.getNormalizedMarker());

        CitationContext context2 = new CitationContext("[1]", "text", "source");
        assertEquals("1", context2.getNormalizedMarker());

        CitationContext context3 = new CitationContext("Smith et al. (2020)", "text", "source");
        assertEquals("Smith et al. 2020", context3.getNormalizedMarker());
    }

    @Test
    void blankMarkerThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new CitationContext("  ", "context", "source"));
    }
}
