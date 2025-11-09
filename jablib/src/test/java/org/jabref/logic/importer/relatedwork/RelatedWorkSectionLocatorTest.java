package org.jabref.logic.importer.relatedwork;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelatedWorkSectionLocatorTest {

    @Test
    void findsRelatedWorkHeader_UppercaseAndStopsAtNextHeader() {
        String txt = """
                1 Introduction
                Some intro text.

                2 RELATED WORK
                Prior studies showed X and Y.
                Even more prior-art discussion.

                3 Methods
                Details here.
                """;

        Optional<RelatedWorkSectionLocator.SectionSpan> opt = RelatedWorkSectionLocator.locateStatic(txt);
        assertTrue(opt.isPresent());

        RelatedWorkSectionLocator.SectionSpan span = opt.get();
        String sectionBody = txt.substring(span.startOffset, span.endOffset);
        String header = headerLineOf(txt, span.startOffset);

        // Header recognition
        assertTrue(header.contains("RELATED WORK"));

        // Body includes expected text
        assertTrue(sectionBody.contains("Prior studies showed X and Y."));

        // Cut off at next header
        assertEquals(-1, sectionBody.indexOf("3 Methods"));
        assertEquals(-1, sectionBody.indexOf("3 METHODS"));
    }

    @Test
    void supportsLiteratureReviewVariant_UppercaseAndStopsAtNextHeader() {
        String txt = """
                Background
                Setup text.

                2.1 LITERATURE REVIEW
                We review A and B.
                More discussion.

                3 RESULTS
                Data stuff.
                """;

        Optional<RelatedWorkSectionLocator.SectionSpan> opt = RelatedWorkSectionLocator.locateStatic(txt);
        assertTrue(opt.isPresent());

        RelatedWorkSectionLocator.SectionSpan span = opt.get();
        String sectionBody = txt.substring(span.startOffset, span.endOffset);
        String header = headerLineOf(txt, span.startOffset);

        // Header recognition (variant)
        assertTrue(header.contains("LITERATURE REVIEW"));

        // Body should include first content line after the header
        assertTrue(sectionBody.contains("We review A and B."));

        // Ensure it cut off before the next section header
        assertEquals(-1, sectionBody.indexOf("3 RESULTS"));
    }

    /**
     * Helper: derive the header line immediately preceding the section body start.
     */
    private static String headerLineOf(String text, int bodyStartOffset) {
        int nlBeforeBody = text.lastIndexOf('\n', Math.max(0, bodyStartOffset - 1));
        if (nlBeforeBody < 0) {
            return text.substring(0, bodyStartOffset).trim();
        }
        int nlBeforeHeader = text.lastIndexOf('\n', Math.max(0, nlBeforeBody - 1));
        return text.substring(nlBeforeHeader + 1, nlBeforeBody).trim();
    }
}
