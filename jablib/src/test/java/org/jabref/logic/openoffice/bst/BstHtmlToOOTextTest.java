package org.jabref.logic.openoffice.bst;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BstHtmlToOOTextTest {

    // --- paragraph wrapper stripping (Bug 1a) ---

    @Test
    void singleParagraphWrapperIsStripped() {
        String input = "<p>K. A. Cooper et al., <em>Br. J. Nutr.</em>, 2007.</p>";
        String result = BstHtmlToOOText.convert(input);

        assertFalse(result.startsWith("<p>"), "Opening <p> wrapper should be stripped");
        assertFalse(result.endsWith("</p>"), "Closing </p> wrapper should be stripped");
        assertTrue(result.contains("Cooper"), "Content should be preserved");
    }

    @Test
    void singleParagraphContentIsKeptInline() {
        // The entry body must be an inline run — no leading paragraph break before the text,
        // so that "[n] " prepended by insertBibliography stays on the same line.
        String input = "<p>Smith et al., 2016.</p>";
        String result = BstHtmlToOOText.convert(input);

        assertEquals("Smith et al., 2016.", result);
    }

    @Test
    void multiParagraphBoundaryBecomesEmptyParagraphSeparator() {
        // Internal </p><p> should become <p></p> (the empty-paragraph separator that
        // OOTextIntoOO turns into a paragraph break, not the content-stripping bug).
        String input = "<p>First sentence.</p>\n<p>Second sentence.</p>";
        String result = BstHtmlToOOText.convert(input);

        assertTrue(result.contains("<p></p>"), "Internal paragraph boundary should become <p></p>");
        assertTrue(result.contains("First sentence."), "First paragraph content preserved");
        assertTrue(result.contains("Second sentence."), "Second paragraph content preserved");
        assertFalse(result.startsWith("<p>"), "Outer opening wrapper should be stripped");
        assertFalse(result.endsWith("</p>"), "Outer closing wrapper should be stripped");
    }

    // --- inline tag conversion ---

    @Test
    void emIsConvertedToI() {
        String input = "<p>A <em>journal</em> title.</p>";
        String result = BstHtmlToOOText.convert(input);

        assertFalse(result.contains("<em>"), "<em> should be converted to <i>");
        assertFalse(result.contains("</em>"), "</em> should be converted to </i>");
        assertTrue(result.contains("<i>journal</i>"), "italic content should be wrapped in <i>");
    }

    @Test
    void strongIsConvertedToB() {
        String input = "<p>A <strong>bold</strong> word.</p>";
        String result = BstHtmlToOOText.convert(input);

        assertFalse(result.contains("<strong>"), "<strong> should be converted to <b>");
        assertFalse(result.contains("</strong>"), "</strong> should be converted to </b>");
        assertTrue(result.contains("<b>bold</b>"), "bold content should be wrapped in <b>");
    }

    @Test
    void smallcapsSpanIsConverted() {
        String input = "<p>A <span class=\"smallcaps\">Smallcaps</span> word.</p>";
        String result = BstHtmlToOOText.convert(input);

        assertFalse(result.contains("<span"), "span should be converted");
        assertTrue(result.contains("<smallcaps>Smallcaps</smallcaps>"), "smallcaps content should be in <smallcaps>");
    }

    // --- real-world pandoc output shapes ---

    @Test
    void typicalIEEEtranPandocOutputIsConverted() {
        // Realistic pandoc output for an IEEEtran entry
        String pandocHtml =
                "<p>K. A. Cooper, J. L. Donovan, A. L. Waterhouse, and G. Williamson, "
                + "&#x201C;Cocoa and health: a decade of research,&#x201D; "
                + "<em>British Journal of Nutrition</em>, vol. 99, no. 1, pp. 1–11, 2007.</p>\n";

        String result = BstHtmlToOOText.convert(pandocHtml);

        assertFalse(result.startsWith("<p>"), "No leading <p>");
        assertTrue(result.contains("Cooper"), "Author preserved");
        assertTrue(result.contains("<i>British Journal of Nutrition</i>"), "Journal in italics");
        assertTrue(result.contains("2007"), "Year preserved");
    }

    @Test
    void typicalAbbrvPandocOutputIsConverted() {
        // abbrv wraps booktitle in {\em ...}; pandoc turns {\em X} into <em>X</em>
        String pandocHtml =
                "<p>K. Crowston, H. Annabi, J. Howison, and C. Masango. "
                + "Effective work practices for floss development. "
                + "In <em>Hawaii Intl. Conference On System Sciences</em>, 2005.</p>\n";

        String result = BstHtmlToOOText.convert(pandocHtml);

        assertFalse(result.startsWith("<p>"), "No leading <p>");
        assertTrue(result.contains("Crowston"), "Author preserved");
        assertTrue(result.contains("<i>Hawaii"), "Booktitle in italics");
    }

    @Test
    void emptyInputDoesNotThrow() {
        String result = BstHtmlToOOText.convert("");
        assertTrue(result.isEmpty() || result.isBlank(), "Empty input should produce empty output");
    }

    @Test
    void whitespaceOnlyInputDoesNotThrow() {
        String result = BstHtmlToOOText.convert("   \n  ");
        assertTrue(result.isBlank(), "Whitespace-only input should produce blank output");
    }
}
