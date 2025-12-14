package org.jabref.model.citation;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CitationContextTest {

    @Test
    void testBasicCreation() {
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
    void testCreationWithPageNumber() {
        CitationContext context = new CitationContext(
                "[1]",
                "Some context text",
                "Source2024",
                5
        );

        assertEquals(Optional.of(5), context.pageNumber());
    }

    @Test
    void testFormatForComment() {
        CitationContext context = new CitationContext(
                "(Smith 2020)",
                "This paper discusses important findings.",
                "MyPaper2024"
        );

        assertEquals("[MyPaper2024]: This paper discusses important findings.", context.formatForComment());
    }

    @Test
    void testGetNormalizedMarker() {
        CitationContext context1 = new CitationContext("(Smith 2020)", "text", "source");
        assertEquals("Smith 2020", context1.getNormalizedMarker());

        CitationContext context2 = new CitationContext("[1]", "text", "source");
        assertEquals("1", context2.getNormalizedMarker());

        CitationContext context3 = new CitationContext("Smith et al. (2020)", "text", "source");
        assertEquals("Smith et al. 2020", context3.getNormalizedMarker());
    }

    @Test
    void testNullMarkerThrows() {
        assertThrows(NullPointerException.class, () ->
                new CitationContext(null, "context", "source"));
    }

    @Test
    void testBlankMarkerThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new CitationContext("  ", "context", "source"));
    }

    @Test
    void testNullContextThrows() {
        assertThrows(NullPointerException.class, () ->
                new CitationContext("(Smith 2020)", null, "source"));
    }

    @Test
    void testNullSourceKeyThrows() {
        assertThrows(NullPointerException.class, () ->
                new CitationContext("(Smith 2020)", "context", null));
    }
}
