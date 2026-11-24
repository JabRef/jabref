package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HTMLCharsTest {

    private LayoutFormatter layout;
    private ParamLayoutFormatter layoutKeep;

    @BeforeEach
    void setUp() {
        layout = new HTMLChars();
        layoutKeep = new HTMLChars();
        layoutKeep.setArgument("keepCurlyBraces");
    }

    private static Stream<Arguments> provideBasicFormattingData() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("hallo", "hallo"),
                Arguments.of("Réflexions sur le timing de la quantité", "Réflexions sur le timing de la quantité"),
                Arguments.of("%%%", "\\%\\%\\%"),
                Arguments.of("People remember 10%, 20%…Oh Really?", "{{People remember 10\\%, 20\\%…Oh Really?}}"),
                Arguments.of("h&aacute;llo", "h\\'allo"),
                Arguments.of("&imath; &imath;", "\\i \\i"),
                Arguments.of("&imath;", "\\i"),
                Arguments.of("{i", "\\{i}"),
                Arguments.of("&imath;&imath;", "\\i\\i"),
                Arguments.of("&auml;", "{\\\"{a}}"),
                Arguments.of("&auml;", "{\\\"a}"),
                Arguments.of("&auml;", "\\\"a"),
                Arguments.of("&Ccedil;", "{\\c{C}}"),
                Arguments.of("&Ccedil;", "\\c{C}"),
                Arguments.of("&Oogon;&imath;", "\\k{O}\\i"),
                Arguments.of("&ntilde; &ntilde; &iacute; &imath; &imath;", "\\~{n} \\~n \\'i \\i \\i")
        );
    }

    @ParameterizedTest
    @MethodSource("provideBasicFormattingData")
    void basicFormat(String expected, String input) {
        assertEquals(expected, layout.format(input));
    }

    private static Stream<Arguments> provideLaTeXHighlightingFormattingData() {
        return Stream.of(
                Arguments.of("<em>hallo</em>", "\\emph{hallo}"),
                Arguments.of("<em>hallo</em>", "{\\emph hallo}"),
                Arguments.of("<em>hallo</em>", "{\\em hallo}"),

                Arguments.of("<i>hallo</i>", "\\textit{hallo}"),
                Arguments.of("<i>hallo</i>", "{\\textit hallo}"),
                Arguments.of("<i>hallo</i>", "{\\it hallo}"),

                Arguments.of("<b>hallo</b>", "\\textbf{hallo}"),
                Arguments.of("<b>hallo</b>", "{\\textbf hallo}"),
                Arguments.of("<b>hallo</b>", "{\\bf hallo}"),

                Arguments.of("<sup>hallo</sup>", "\\textsuperscript{hallo}"),
                Arguments.of("<sub>hallo</sub>", "\\textsubscript{hallo}"),

                Arguments.of("<u>hallo</u>", "\\underline{hallo}"),
                Arguments.of("<s>hallo</s>", "\\sout{hallo}"),
                Arguments.of("<tt>hallo</tt>", "\\texttt{hallo}"),

                Arguments.of("<span style=\"font-variant: small-caps\">hallo</span>", "\\textsc{hallo}"),
                Arguments.of("<span style=\"font-variant: small-caps\">hallo</span>", "{\\sc hallo}"),
                Arguments.of("<span style=\"font-variant: small-caps\">hallo</span>", "{\\scshape hallo}"),
                Arguments.of("<span style=\"font-variant: small-caps\">hallo</span>", "\\textsc {hallo}"),

                Arguments.of("<em>L&oacute;pez</em>", "\\emph{L{\\'o}pez}"),
                Arguments.of("<em>The TeX Book</em>", "{\\em The {\\TeX} Book}"),
                Arguments.of("<b>a <i>b</i> c</b>", "\\textbf{a \\textit{b} c}"),
                Arguments.of("hallo", "\\textsc\\hallo")
        );
    }

    @ParameterizedTest
    @MethodSource("provideLaTeXHighlightingFormattingData")
    void laTeXHighlighting(String expected, String input) {
        assertEquals(expected, layout.format(input));
    }

    private static Stream<Arguments> provideEquationTestData() {
        return Stream.of(
                Arguments.of("&dollar;", "\\$"),
                Arguments.of("&sigma;", "$\\sigma$"),
                Arguments.of("A 32&nbsp;mA &Sigma;&Delta;-modulator", "A 32~{mA} {$\\Sigma\\Delta$}-modulator")
        );
    }

    @ParameterizedTest
    @MethodSource("provideEquationTestData")
    void equations(String expected, String input) {
        assertEquals(expected, layout.format(input));
    }

    private static Stream<Arguments> provideNewLineTestData() {
        return Stream.of(
                Arguments.of("a<br>b", "a\nb"),
                Arguments.of("a<p>b", "a\n\nb")
        );
    }

    @ParameterizedTest
    @MethodSource("provideNewLineTestData")
    void newLine(String expected, String input) {
        assertEquals(expected, layout.format(input));
    }

    /*
     * Is missing a lot of test cases for the individual chars...
     */

    @Test
    void quoteSingle() {
        assertEquals("&#39;", layout.format("{\\textquotesingle}"));
    }

    @Test
    void unknownCommandIsKept() {
        assertEquals("aaaa", layout.format("\\aaaa"));
    }

    @Test
    void unknownCommandKeepsArgument() {
        assertEquals("bbbb", layout.format("\\aaaa{bbbb}"));
    }

    @Test
    void unknownCommandWithEmptyArgumentIsKept() {
        assertEquals("aaaa", layout.format("\\aaaa{}"));
    }

    private static Stream<Arguments> provideHTMLEntityFormattingData() {
        return Stream.of(
                Arguments.of("&amp;", "&"),
                Arguments.of("&gt;", "&gt;"),
                Arguments.of("&Omega;", "&Omega;"),
                Arguments.of("&Alpha;", "&Alpha;"),
                Arguments.of("&alpha;", "&alpha;"),
                Arguments.of("&amp;HelloWorld", "&HelloWorld"),
                Arguments.of("&amp;amp;", "\\\\&"),
                Arguments.of("&lt;", "&lt;"),
                Arguments.of("&gt;", "&gt;")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHTMLEntityFormattingData")
    void hTMLEntityFormatting(String expected, String input) {
        assertEquals(expected, layout.format(input));
    }

    private static Stream<Arguments> provideBracesKeepFormattingData() {
        return Stream.of(
                Arguments.of("{&auml;}", "{\\\"{a}}"),
                Arguments.of("{&auml;}", "{\\\"a}"),
                Arguments.of("{&Ccedil;}", "{\\c{C}}"),
                Arguments.of("{<em>hallo</em>}", "{\\emph hallo}"),
                Arguments.of("{<b>hallo</b>}", "{\\textbf hallo}"),
                Arguments.of("{<b>hallo</b>}", "{\\bf hallo}"),
                Arguments.of("{&#39;}", "{\\textquotesingle}"),
                Arguments.of("{{ test }}", "{{ test }}")
        );
    }

    @ParameterizedTest
    @MethodSource("provideBracesKeepFormattingData")
    void bracesKeepFormatting(String expected, String input) {
        assertEquals(expected, layoutKeep.format(input));
    }

    private static Stream<Arguments> provideNestedBracesKeepFormattingData() {
        return Stream.of(
                Arguments.of(
                        "<i>{British} <b>{Journal}</b> of {Nutrition}</i>",
                        "\\textit{{British} \\textbf{{Journal}} of {Nutrition}}"),
                Arguments.of("{<i>{British}</i>}", "{\\textit{{British}}}"),
                Arguments.of(
                        "{<em>British {<b>Journal</b>} of Nutrition</em>}",
                        "{\\em British {\\bf Journal} of Nutrition}"),
                Arguments.of(
                        "<span style=\"font-variant: small-caps\">{Small} <b>caps</b></span> tail",
                        "\\textsc{{Small} \\textbf{caps}} tail"),
                Arguments.of(
                        "{<span style=\"font-variant: small-caps\">Small caps</span>}",
                        "{\\scshape Small caps}"),
                Arguments.of("<i>M{&uuml;}ller <b>caf&eacute;</b></i>", "\\textit{M{\\\"{u}}ller \\textbf{caf\\'{e}}}")
        );
    }

    @ParameterizedTest
    @MethodSource("provideNestedBracesKeepFormattingData")
    void nestedFormattingKeepsProtectionBraces(String expected, String input) {
        assertEquals(expected, layoutKeep.format(input));
    }

    private static Stream<Arguments> provideEscapedLiteralBracesData() {
        return Stream.of(
                Arguments.of("{literal}", "\\{literal\\}"),
                Arguments.of("{i}", "\\{i\\}"),
                Arguments.of("{", "\\{"),
                Arguments.of("}", "\\}"),
                Arguments.of("<i>a {literal} z</i> tail", "\\textit{a \\{literal\\} z} tail"),
                Arguments.of("<i>a {literal z</i> tail", "\\textit{a \\{literal z} tail"),
                Arguments.of("<i>a }literal z</i> tail", "\\textit{a \\}literal z} tail"),
                Arguments.of("<i>a {<b>literal</b>} z</i>", "\\textit{a \\{\\textbf{literal}\\} z}")
        );
    }

    @ParameterizedTest
    @MethodSource("provideEscapedLiteralBracesData")
    void escapedLiteralBracesArePreservedInBothBraceModes(String expected, String input) {
        assertEquals(expected, layout.format(input));
        assertEquals(expected, layoutKeep.format(input));
    }
}
