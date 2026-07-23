package org.jabref.logic.openoffice.bst;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests for [BstStyleUtils.transformHTML] — the general HTML-to-OOText cleanup used by the BST path.
///
/// These tests cover only operations that apply to pandoc HTML. CSL-exclusive operations
/// (csl-left-margin div merge, citeproc-java span styles) are tested in CSLFormatUtilsTest.
class BstStyleUtilsTest {

    @ParameterizedTest
    @MethodSource
    void transformHTML(String expected, String input) {
        assertEquals(expected, BstStyleUtils.transformHTML(input));
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

                // --- combinations typical of pandoc BST output (after BstHtmlToOOText pre-processing) ---

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
}
