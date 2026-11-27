package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class LatexMarkupConverterTest {

    private static Stream<Arguments> sharedFormattingData() {
        return Stream.of(
                Arguments.of(
                        "\\textit{British \\textbf{Journal}ofNutrition}",
                        "<i>British <b>Journal</b>ofNutrition</i>",
                        "<i>British <b>Journal</b>ofNutrition</i>"),
                Arguments.of(
                        "\\textit{British \\textbf{Journal} of Nutrition} tail",
                        "<i>British <b>Journal</b> of Nutrition</i> tail",
                        "<i>British <b>Journal</b> of Nutrition</i> tail"),
                Arguments.of(
                        "\\emph{British \\textbf{Journal} of Nutrition}",
                        "<em>British <b>Journal</b> of Nutrition</em>",
                        "<i>British <b>Journal</b> of Nutrition</i>"),
                Arguments.of(
                        "\\textbf{bold \\textit{italic \\underline{text}}}",
                        "<b>bold <i>italic <u>text</u></i></b>",
                        "<b>bold <i>italic <u>text</u></i></b>"),
                Arguments.of(
                        "\\textit{{British} \\textbf{{Journal}} of {Nutrition}}",
                        "<i>British <b>Journal</b> of Nutrition</i>",
                        "<i>British <b>Journal</b> of Nutrition</i>"),
                Arguments.of("{\\textit{{British}}}", "<i>British</i>", "<i>British</i>"),
                Arguments.of("\\textsc\\hallo", "hallo", "hallo"),
                Arguments.of("\\emph", "", ""),
                Arguments.of("\\^{q}", "q", ""),
                Arguments.of("\\^q", "q", ""),
                Arguments.of("\\c{q}", "q", "q"),
                Arguments.of("\\^& B", "&amp; B", " B"),
                Arguments.of("\\^&{copy;}", "&amp;copy;", "copy;"),
                Arguments.of("\\unknown{\\^&{copy;}}", "&amp;copy;", "copy;"),
                Arguments.of("\\textit{a {\\bf b", "<i>a <b>b</b></i>", "<i>a <b>b</b></i>"),
                Arguments.of("\\unknown{\\textit{x", "<i>x</i>", "<i>x</i>"),
                Arguments.of("\\textit {word} tail", "<i>word</i> tail", "<i>word</i> tail"),
                Arguments.of("\\textit \t{word} tail", "<i>word</i> tail", "<i>word</i> tail"),
                Arguments.of("\\emph {British} Journal", "<em>British</em> Journal", "<i>British</i> Journal"),
                Arguments.of("{\\em {British} Journal} tail", "<em>British Journal</em> tail", "<i>British Journal</i> tail"),
                Arguments.of("\\textsc {Small} tail", "<span style=\"font-variant: small-caps\">Small</span> tail", "<smallcaps>Small</smallcaps> tail"),
                Arguments.of("{\\sc {Small} caps} end", "<span style=\"font-variant: small-caps\">Small caps</span> end", "<smallcaps>Small caps</smallcaps> end"),
                Arguments.of("\\{literal\\}", "{literal}", "{literal}"),
                Arguments.of("\\{i\\}", "{i}", "{i}"),
                Arguments.of("\\{", "{", "{"),
                Arguments.of("\\}", "}", "}"),
                Arguments.of(
                        "\\textit{a \\{literal\\} z} tail",
                        "<i>a {literal} z</i> tail",
                        "<i>a {literal} z</i> tail"),
                Arguments.of(
                        "\\textit{a \\{literal z} tail",
                        "<i>a {literal z</i> tail",
                        "<i>a {literal z</i> tail"),
                Arguments.of(
                        "\\textit{a \\}literal z} tail",
                        "<i>a }literal z</i> tail",
                        "<i>a }literal z</i> tail"),
                Arguments.of(
                        "\\textit{a \\{\\textbf{literal}\\} z}",
                        "<i>a {<b>literal</b>} z</i>",
                        "<i>a {<b>literal</b>} z</i>"),
                Arguments.of(
                        "\\textit{M{\\\"{u}}ller \\textbf{caf\\'{e}}}",
                        "<i>M&uuml;ller <b>caf&eacute;</b></i>",
                        "<i>Müller <b>café</b></i>"),
                Arguments.of(
                        "\\textit{cost \\textbf{\\$5} and $x$}",
                        "<i>cost <b>&dollar;5</b> and x</i>",
                        "<i>cost <b>$5</b> and x</i>"),
                Arguments.of(
                        "\\textit{\\textbf{\\$}x\\$}",
                        "<i><b>&dollar;</b>x&dollar;</i>",
                        "<i><b>$</b>x$</i>"),
                Arguments.of(
                        "{\\em British {\\bf Journal} of Nutrition}",
                        "<em>British <b>Journal</b> of Nutrition</em>",
                        "<i>British <b>Journal</b> of Nutrition</i>"),
                Arguments.of("{\\bf bold}", "<b>bold</b>", "<b>bold</b>"),
                Arguments.of(
                        "{\\sc Small caps}",
                        "<span style=\"font-variant: small-caps\">Small caps</span>",
                        "<smallcaps>Small caps</smallcaps>"),
                Arguments.of(
                        "{\\scshape Small caps}",
                        "<span style=\"font-variant: small-caps\">Small caps</span>",
                        "<smallcaps>Small caps</smallcaps>"),
                Arguments.of(
                        "\\textsc{Small \\textbf{bold} caps} tail",
                        "<span style=\"font-variant: small-caps\">Small <b>bold</b> caps</span> tail",
                        "<smallcaps>Small <b>bold</b> caps</smallcaps> tail"),
                Arguments.of(
                        "\\textbf{bold \\textsc{Small} tail}",
                        "<b>bold <span style=\"font-variant: small-caps\">Small</span> tail</b>",
                        "<b>bold <smallcaps>Small</smallcaps> tail</b>"),
                Arguments.of(
                        "\\textsc{outer \\textsc{inner} tail} end",
                        "<span style=\"font-variant: small-caps\">outer <span style=\"font-variant: small-caps\">inner</span> tail</span> end",
                        "<smallcaps>outer <smallcaps>inner</smallcaps> tail</smallcaps> end"),
                Arguments.of(
                        "\\textit{{\\sc Small} {\\scshape caps}}",
                        "<i><span style=\"font-variant: small-caps\">Small</span> <span style=\"font-variant: small-caps\">caps</span></i>",
                        "<i><smallcaps>Small</smallcaps> <smallcaps>caps</smallcaps></i>")
        );
    }

    // [utest->req~openoffice.jstyle-nested-latex-formatting~1]
    @ParameterizedTest
    @MethodSource("sharedFormattingData")
    void publicAdaptersConvertNestedFormatting(String input, String expectedHtml, String expectedWriter) {
        assertAll(
                () -> assertEquals(expectedHtml, new HTMLChars().format(input)),
                () -> assertEquals(expectedWriter, new OOPreFormatter().format(input))
        );
    }

    private static Stream<Arguments> adapterNormalizationData() {
        return Stream.of(
                Arguments.of("A & B", "A &amp; B", "A &amp; B"),
                Arguments.of("A \\& B", "A &amp; B", "A &amp; B"),
                Arguments.of("A~B", "A&nbsp;B", "A~B"),
                Arguments.of("a\nb", "a<br>b", "a\nb"),
                Arguments.of("a\n\nb", "a<p>b", "a\n\nb"),
                Arguments.of("\\$", "&dollar;", "$"),
                Arguments.of("$\\Sigma$", "&Sigma;", "Σ"),
                Arguments.of("&amp; &nbsp;", "&amp; &nbsp;", "&amp; &nbsp;"),
                Arguments.of(
                        "\\textit{A \\textbf{\\& B}~C\nD\n\nE}",
                        "<i>A <b>&amp; B</b>&nbsp;C<br>D<p>E</i>",
                        "<i>A <b>&amp; B</b>~C\nD\n\nE</i>"),
                Arguments.of(
                        "<i>British <b>Journal</b> of Nutrition</i>",
                        "<i>British <b>Journal</b> of Nutrition</i>",
                        "<i>British <b>Journal</b> of Nutrition</i>"),
                Arguments.of(
                        "\\textit{British <b>Journal</b>}",
                        "<i>British <b>Journal</b></i>",
                        "<i>British <b>Journal</b></i>"),
                Arguments.of(
                        "<span style=\"font-variant: small-caps\"><b>Small</b> caps</span>",
                        "<span style=\"font-variant: small-caps\"><b>Small</b> caps</span>",
                        "<span style=\"font-variant: small-caps\"><b>Small</b> caps</span>")
        );
    }

    @ParameterizedTest
    @MethodSource("adapterNormalizationData")
    void publicAdaptersPreserveTheirNormalization(String input, String expectedHtml, String expectedWriter) {
        assertAll(
                () -> assertEquals(expectedHtml, new HTMLChars().format(input)),
                () -> assertEquals(expectedWriter, new OOPreFormatter().format(input))
        );
    }

    private static Stream<Arguments> controlSymbolData() {
        return Stream.of(
                Arguments.of("a\\, b", "a\u2009 b", "a\u2009 b"),
                Arguments.of("a\\,b", "a\u2009b", "a\u2009b"),
                Arguments.of("a\\;b", "a\u2005b", "a\u2005b"),
                Arguments.of("a\\!b", "ab", "ab"),
                Arguments.of("a\\:b", "a\u205Fb", "a\u205Fb"),
                Arguments.of("a\\ b", "a b", "a b"),
                Arguments.of("a\\,{b}", "a\u2009b", "a\u2009b"),
                Arguments.of("a\\;{b}", "a\u2005b", "a\u2005b"),
                Arguments.of("a\\!{b}", "ab", "ab"),
                Arguments.of("a\\:{b}", "a\u205Fb", "a\u205Fb"),
                Arguments.of("a\\ {b}", "a b", "a b"),
                Arguments.of("\\,", "\u2009", "\u2009"),
                Arguments.of("\\;", "\u2005", "\u2005"),
                Arguments.of("\\!", "", ""),
                Arguments.of("\\:", "\u205F", "\u205F"),
                Arguments.of("a,;!:b", "a,;!:b", "a,;!:b"),
                Arguments.of("a\\%b", "a%b", "a%b"),
                Arguments.of("a\\_b", "a_b", "a_b"),
                Arguments.of("a\\#b", "a#b", "a#b"),
                Arguments.of("a\\_{b}", "a_b", "a_b"),
                Arguments.of("a\\#{b}", "a#b", "a#b")
        );
    }

    @ParameterizedTest
    @MethodSource("controlSymbolData")
    void publicAdaptersPreserveControlSymbolPolicies(String input, String expectedHtml, String expectedWriter) {
        assertAll(
                () -> assertEquals(expectedHtml, new HTMLChars().format(input)),
                () -> assertEquals(expectedWriter, new OOPreFormatter().format(input)),
                () -> assertEquals("<i>" + expectedHtml + "</i>", new HTMLChars().format("\\textit{" + input + "}")),
                () -> assertEquals("<i>" + expectedWriter + "</i>", new OOPreFormatter().format("\\textit{" + input + "}")),
                () -> assertEquals("<i><b>" + expectedHtml + "</b></i>", new HTMLChars().format("\\textit{\\textbf{" + input + "}}")),
                () -> assertEquals("<i><b>" + expectedWriter + "</b></i>", new OOPreFormatter().format("\\textit{\\textbf{" + input + "}}"))
        );
    }

    @Test
    void htmlControlSymbolsDoNotConsumeFollowingProtectionGroups() {
        HTMLChars formatter = new HTMLChars();
        formatter.setArgument("keepCurlyBraces");

        assertAll(
                () -> assertEquals("a\u2009{b}", formatter.format("a\\,{b}")),
                () -> assertEquals("a {b}", formatter.format("a\\ {b}")),
                () -> assertEquals("a_{b}", formatter.format("a\\_{b}")),
                () -> assertEquals("a#{b}", formatter.format("a\\#{b}"))
        );
    }

    private static Stream<Arguments> ampersandData() {
        return Stream.of(
                Arguments.of("&", "&amp;", "&"),
                Arguments.of("\\&", "&amp;", "&"),
                Arguments.of("&{copy;}", "&amp;copy;", "&copy;"),
                Arguments.of("&\\unknown{copy;}", "&amp;copy;", "&copy;"),
                Arguments.of("&co{}py;", "&amp;copy;", "&copy;"),
                Arguments.of("\\&copy;", "&amp;copy;", "&copy;"),
                Arguments.of("\\&amp;", "&amp;amp;", "&amp;"),
                Arguments.of("\\&#38;", "&amp;#38;", "&#38;"),
                Arguments.of("\\&{copy;}", "&amp;copy;", "&copy;"),
                Arguments.of("&amp;", "&amp;", "&"),
                Arguments.of("&copy;", "&copy;", "©"),
                Arguments.of("&#38;", "&#38;", "&"),
                Arguments.of("&#x26;", "&#x26;", "&"),
                Arguments.of("&amp;copy;", "&amp;copy;", "&copy;"),
                Arguments.of("\\unknown{A & B}", "A &amp; B", "A & B"),
                Arguments.of("\\unknown{A \\& B}", "A &amp; B", "A & B")
        );
    }

    @ParameterizedTest
    @MethodSource("ampersandData")
    void publicAdaptersProtectLiteralAmpersands(String input, String expectedMarkup, String expectedText) {
        String html = new HTMLChars().format(input);
        String writer = new OOPreFormatter().format(input);

        assertAll(
                () -> assertEquals(expectedMarkup, html),
                () -> assertEquals(expectedMarkup, writer),
                () -> assertEquals(expectedText, StringEscapeUtils.unescapeHtml4(html)),
                () -> assertEquals(expectedText, StringEscapeUtils.unescapeHtml4(writer)),
                () -> assertEquals("<i>" + expectedMarkup + "</i>", new HTMLChars().format("\\textit{" + input + "}")),
                () -> assertEquals("<i>" + expectedMarkup + "</i>", new OOPreFormatter().format("\\textit{" + input + "}"))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void publicAdaptersRespectBackslashParityBeforeAmpersands(int backslashes) {
        String prefix = "\\".repeat(backslashes);
        String expectedPrefix = "\\".repeat(backslashes / 2);

        assertAll(
                () -> assertEquals(expectedPrefix + "&amp;", new HTMLChars().format(prefix + "&")),
                () -> assertEquals(expectedPrefix + "&amp;", new OOPreFormatter().format(prefix + "&")),
                () -> assertEquals(expectedPrefix + "&amp;b", new HTMLChars().format(prefix + "&{b}")),
                () -> assertEquals(expectedPrefix + "&amp;b", new OOPreFormatter().format(prefix + "&{b}")),
                () -> assertEquals("<i>" + expectedPrefix + "&amp;</i>", new HTMLChars().format("\\textit{" + prefix + "&}")),
                () -> assertEquals("<i>" + expectedPrefix + "&amp;</i>", new OOPreFormatter().format("\\textit{" + prefix + "&}"))
        );
    }

    private static Stream<String> deeplyNestedGroupInputs() {
        return Stream.of(
                "{".repeat(3000) + "x" + "}".repeat(3000),
                "{".repeat(3000) + "x"
        );
    }

    private static Stream<Arguments> deeplyNestedGroupsDiscardingBracesData() {
        return deeplyNestedGroupInputs().flatMap(input -> Stream.of(
                Arguments.of(new HTMLChars(), input),
                Arguments.of(new OOPreFormatter(), input)
        ));
    }

    @ParameterizedTest
    @MethodSource("deeplyNestedGroupsDiscardingBracesData")
    void publicAdaptersDiscardDeeplyNestedGroupsWithoutStackOverflow(LayoutFormatter formatter, String input) {
        assertEquals("x", formatter.format(input));
    }

    @ParameterizedTest
    @MethodSource("deeplyNestedGroupInputs")
    void htmlKeepsDeeplyNestedBalancedAndHangingGroupBracesWithoutStackOverflow(String input) {
        HTMLChars formatter = new HTMLChars();
        formatter.setArgument("keepCurlyBraces");

        assertEquals(input, formatter.format(input));
    }

    @Test
    void publicAdaptersConvertDeeplyNestedStylesWithoutStackOverflow() {
        String input = "\\emph{".repeat(1000) + "x" + "}".repeat(1000);
        String expectedHtml = "<em>".repeat(1000) + "x" + "</em>".repeat(1000);
        String expectedWriter = "<i>".repeat(1000) + "x" + "</i>".repeat(1000);
        HTMLChars keepingBraces = new HTMLChars();
        keepingBraces.setArgument("keepCurlyBraces");

        assertAll(
                () -> assertEquals(expectedHtml, new HTMLChars().format(input)),
                () -> assertEquals(expectedWriter, new OOPreFormatter().format(input)),
                () -> assertEquals(expectedHtml, keepingBraces.format(input))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", "long payload ", "\\textit{nested}"})
    void publicAdaptersConvertDeeplyNestedUnknownArguments(String payload) {
        String input = "\\unknown{".repeat(3000) + payload.repeat(1000) + "}".repeat(3000);
        String expected = payload.replace("\\textit{nested}", "<i>nested</i>").repeat(1000);

        assertAll(
                () -> assertEquals(expected, new HTMLChars().format(input)),
                () -> assertEquals(expected, new OOPreFormatter().format(input))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\aaaa{\\relax}", "\\aaaa{ \\relax}"})
    void unknownCommandWithNonemptySourceArgumentRenderingEmptyIsDropped(String input) {
        assertAll(
                () -> assertEquals("", new HTMLChars().format(input)),
                () -> assertEquals("", new OOPreFormatter().format(input))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\aaaa{ }", "\\aaaa{ \t}"})
    void unknownCommandWithWhitespaceOnlyArgumentRetainsAdapterDifference(String input) {
        assertAll(
                () -> assertEquals("aaaa", new HTMLChars().format(input)),
                () -> assertEquals("", new OOPreFormatter().format(input))
        );
    }

    @Test
    void unknownCommandWithEmptyArgumentRetainsAdapterDifference() {
        assertAll(
                () -> assertEquals("aaaa", new HTMLChars().format("\\aaaa{}")),
                () -> assertEquals("", new OOPreFormatter().format("\\aaaa{}"))
        );
    }
}
