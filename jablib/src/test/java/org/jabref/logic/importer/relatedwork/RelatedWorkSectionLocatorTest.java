package org.jabref.logic.importer.relatedwork;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelatedWorkSectionLocatorTest {

    private final RelatedWorkSectionLocator locator = new RelatedWorkSectionLocator();

    @Test
    void findsSimpleRelatedWorkHeader() {
        String txt = """
                1 Introduction
                Some intro text.

                2 Related Work
                Prior studies showed X and Y.

                3 Methods
                """;

        Optional<String> section = locator.locate(txt);
        assertTrue(section.isPresent());
        // Starts with the line after the header, should contain the first sentence
        assertTrue(section.get().contains("Prior studies showed X and Y."));
    }

    @Test
    void supportsLiteratureReviewVariantAndStopsAtNextHeader() {
        String txt = """
                Background
                Setup text.

                2.1 Literature Review
                We review A and B.
                More discussion.

                3 RESULTS
                Data stuff.
                """;

        Optional<String> section = locator.locate(txt);
        assertTrue(section.isPresent());
        String s = section.get();

        assertTrue(s.startsWith("\nWe review A and B."));
        // Ensure it cut off before "3 RESULTS"
        assertEquals(-1, s.indexOf("3 RESULTS"));
    }
}
