package org.jabref.logic.openoffice.bst;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BSTFormatUtilsTest {

    @Test
    void convertsScSwitchToTextsc() {
        String in = "{\\sc Dinga, E.~L., Ding, X.} and more";
        String out = BSTFormatUtils.normalizeLegacyForPandoc(in);
        assertEquals("\\textsc{Dinga, E.~L., Ding, X.} and more", out);
    }

    @Test
    void preservesNestedBracesInsideContent() {
        String in = "{\\sc L{\\'opez}, A.} 2007";
        String out = BSTFormatUtils.normalizeLegacyForPandoc(in);
        assertEquals("\\textsc{L{\\'opez}, A.} 2007", out);
    }

    @Test
    void convertsOtherLegacySwitches() {
        String in = "{\\bf Bold} and {\\it Italic} and {\\em Emph}";
        String out = BSTFormatUtils.normalizeLegacyForPandoc(in);
        assertEquals("\\textbf{Bold} and \\textit{Italic} and \\emph{Emph}", out);
    }

    @Test
    void mapsPandocSmallcapsStyleToOO() {
        String html = "<p><span style=\"font-variant: small-caps\">Name</span></p>";
        String mapped = BSTFormatUtils.mapPandocInlineToOO(html);
        assertTrue(mapped.contains("<smallcaps>Name</smallcaps>"));
    }

    @Test
    void mapsPandocUnderlineStyleToOO() {
        String html = "<p><span style=\"text-decoration: underline\">u</span></p>";
        String mapped = BSTFormatUtils.mapPandocInlineToOO(html);
        assertTrue(mapped.contains("<u>u</u>"));
    }



    @Test
    void convertPandocHtmlToOOTextUnwrapsAndMaps() {
        String html = "<p>A <em>journal</em>, vol. <span style=\"font-variant: small-caps\">X</span></p>";
        String out = BSTFormatUtils.convertPandocHtmlToOOText(html);
        assertTrue(out.startsWith("A "));
        assertTrue(out.contains("<i>journal</i>"));
        assertTrue(out.contains("<smallcaps>X</smallcaps>"));
        assertTrue(!out.startsWith("<p>"));
    }

    // --- transformHTML (migrated from BstStyleUtilsTest) ---

    @ParameterizedTest
    @MethodSource
    void transformHTML(String expected, String input) {
        assertEquals(expected, BSTFormatUtils.transformHTML(input));
    }

    static Stream<Arguments> transformHTML() {
        return Stream.of(
                // --- HTML entity decoding ---
                Arguments.of(
                        "Smith & Jones",
                        "Smith &amp; Jones"
                ),
                // &#x201C; / &#x201D; decode to Unicode left/right double quotation marks
                Arguments.of(
                        "\u201Cquoted\u201D",
                        "&#x201C;quoted&#x201D;"
                ),
                // &nbsp; decodes to a non-breaking space (U+00A0), which is kept as-is
                Arguments.of(
                        "word\u00a0word",
                        "word&nbsp;word"
                ),
                // --- <div> stripping ---
                Arguments.of(
                        "Block content",
                        "<div>Block content</div>"
                ),
                Arguments.of(
                        "Block content",
                        "<div class=\"block\">Block content</div>"
                ),
                // Nested divs (pandoc block quotes)
                Arguments.of(
                        "Inner",
                        "<div><div>Inner</div></div>"
                ),
                // --- <a> stripping ---
                Arguments.of(
                        "Text with link",
                        "Text with <a href=\"https://example.com\">link</a>"
                ),
                // URL text itself is preserved, only the tags are stripped
                Arguments.of(
                        "See https://doi.org/10.1000/example for details",
                        "See <a href=\"https://doi.org/10.1000/example\">https://doi.org/10.1000/example</a> for details"
                ),
                // --- remaining <span> stripping ---
                Arguments.of(
                        "plain text",
                        "<span>plain text</span>"
                ),
                Arguments.of(
                        "plain text",
                        "<span class=\"unknown-class\">plain text</span>"
                ),
                // Already-converted inline tags (<i>, <b>, <smallcaps>) must survive
                Arguments.of(
                        "<i>italic</i> and <b>bold</b>",
                        "<i>italic</i> and <b>bold</b>"
                ),
                // --- newline → <p></p> ---
                Arguments.of(
                        "first<p></p>second",
                        "first\nsecond"
                ),
                Arguments.of(
                        "first<p></p>second",
                        "first\r\nsecond"
                ),
                // Multiple consecutive newlines collapse into one separator
                Arguments.of(
                        "first<p></p>second",
                        "first\n\nsecond"
                ),
                // --- leading empty paragraph stripped ---
                Arguments.of(
                        "text",
                        "<p></p>text"
                ),
                Arguments.of(
                        "text",
                        "  <p>  </p>  text"
                ),
                // --- trailing multiple <p></p> collapsed to one ---
                Arguments.of(
                        "text<p></p>",
                        "text<p></p><p></p>"
                ),
                Arguments.of(
                        "text<p></p>",
                        "text<p></p><p></p><p></p>"
                ),
                // A single trailing <p></p> is kept as-is
                Arguments.of(
                        "text<p></p>",
                        "text<p></p>"
                ),
                // --- trim ---
                Arguments.of(
                        "trimmed",
                        "  trimmed  "
                ),
                // --- combinations typical of pandoc BST output (after BSTFormatUtils pre-processing) ---
                // &#x2013; (en-dash) is decoded to the Unicode en-dash character U+2013
                Arguments.of(
                        "K. A. Cooper et al., <i>British Journal of Nutrition</i>, vol. 99, pp. 1\u201311, 2007.",
                        "K. A. Cooper et al., <i>British Journal of Nutrition</i>, vol. 99, pp. 1&#x2013;11, 2007."
                ),
                // Pre-converted bold + trimmed; en-dash entity decoded
                Arguments.of(
                        "B. Smith, <b>34</b>, 45\u201367 (2016).",
                        "  B. Smith, <b>34</b>, 45&#x2013;67 (2016).  "
                )
        );
    }

    // --- Migrated tests from BstHtmlToOOTextTest ---

    @Test
    void singleParagraphWrapperIsStripped() {
        String input = "<p>K. A. Cooper et al., <em>Br. J. Nutr.</em>, 2007.</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);

        assertFalse(result.startsWith("<p>"), "Opening <p> wrapper should be stripped");
        assertFalse(result.endsWith("</p>"), "Closing </p> wrapper should be stripped");
        assertTrue(result.contains("Cooper"), "Content should be preserved");
    }

    @Test
    void singleParagraphContentIsKeptInline() {
        String input = "<p>Smith et al., 2016.</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);
        assertEquals("Smith et al., 2016.", result);
    }

    @Test
    void multiParagraphBoundaryBecomesEmptyParagraphSeparator() {
        String input = "<p>First sentence.</p>\n<p>Second sentence.</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);

        assertTrue(result.contains("<p></p>"), "Internal paragraph boundary should become <p></p>");
        assertTrue(result.contains("First sentence."), "First paragraph content preserved");
        assertTrue(result.contains("Second sentence."), "Second paragraph content preserved");
        assertFalse(result.startsWith("<p>"), "Outer opening wrapper should be stripped");
        assertFalse(result.endsWith("</p>"), "Outer closing wrapper should be stripped");
    }

    @Test
    void emIsConvertedToI() {
        String input = "<p>A <em>journal</em> title.</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);

        assertFalse(result.contains("<em>"), "<em> should be converted to <i>");
        assertFalse(result.contains("</em>"), "</em> should be converted to </i>");
        assertTrue(result.contains("<i>journal</i>"), "italic content should be wrapped in <i>");
    }

    @Test
    void strongIsConvertedToB() {
        String input = "<p>A <strong>bold</strong> word.</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);

        assertFalse(result.contains("<strong>"), "<strong> should be converted to <b>");
        assertFalse(result.contains("</strong>"), "</strong> should be converted to </b>");
        assertTrue(result.contains("<b>bold</b>"), "bold content should be wrapped in <b>");
    }

    @Test
    void smallcapsSpanIsConverted() {
        String input = "<p>A <span class=\"smallcaps\">Smallcaps</span> word.</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);

        assertFalse(result.contains("<span"), "span should be converted");
        assertTrue(result.contains("<smallcaps>Smallcaps</smallcaps>"), "smallcaps content should be in <smallcaps>");
    }

    @Test
    void smallcapsStyleIsConverted() {
        String input = "<p>A <span style=\"font-variant: small-caps\">Smallcaps</span> word.</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);
        assertTrue(result.contains("<smallcaps>Smallcaps</smallcaps>"));
    }

    @Test
    void underlineStyleIsConvertedToU() {
        String input = "<p>A <span style=\"text-decoration: underline\">u</span> word.</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);
        assertTrue(result.contains("<u>u</u>"));
    }

    @Test
    void superscriptIsPreserved() {
        String input = "<p>H<sup>2</sup>O</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);
        assertTrue(result.contains("H<sup>2</sup>O"));
    }

    @Test
    void ampersandEntityDecodes() {
        String input = "<p>R&amp;D</p>";
        String result = BSTFormatUtils.convertPandocHtmlToOOText(input);
        assertTrue(result.contains("R&D"));
    }

    @Test
    void typicalIEEEtranPandocOutputIsConverted() {
        String pandocHtml =
                "<p>K. A. Cooper, J. L. Donovan, A. L. Waterhouse, and G. Williamson, "
                        + "&#x201C;Cocoa and health: a decade of research,&#x201D; "
                        + "<em>British Journal of Nutrition</em>, vol. 99, no. 1, pp. 1–11, 2007.</p>\n";

        String result = BSTFormatUtils.convertPandocHtmlToOOText(pandocHtml);

        assertFalse(result.startsWith("<p>"), "No leading <p>");
        assertTrue(result.contains("Cooper"), "Author preserved");
        assertTrue(result.contains("<i>British Journal of Nutrition</i>"), "Journal in italics");
        assertTrue(result.contains("2007"), "Year preserved");
    }

    @Test
    void typicalAbbrvPandocOutputIsConverted() {
        String pandocHtml =
                "<p>K. Crowston, H. Annabi, J. Howison, and C. Masango. "
                        + "Effective work practices for floss development. "
                        + "In <em>Hawaii Intl. Conference On System Sciences</em>, 2005.</p>\n";

        String result = BSTFormatUtils.convertPandocHtmlToOOText(pandocHtml);

        assertFalse(result.startsWith("<p>"), "No leading <p>");
        assertTrue(result.contains("Crowston"), "Author preserved");
        assertTrue(result.contains("<i>Hawaii"), "Booktitle in italics");
    }

    @Test
    void emptyInputDoesNotThrow() {
        String result = BSTFormatUtils.convertPandocHtmlToOOText("");
        assertTrue(result.isEmpty() || result.isBlank(), "Empty input should produce empty output");
    }

    @Test
    void whitespaceOnlyInputDoesNotThrow() {
        String result = BSTFormatUtils.convertPandocHtmlToOOText("   \n  ");
        assertTrue(result.isBlank(), "Whitespace-only input should produce blank output");
    }
}
