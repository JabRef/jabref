package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HTMLCharsTest {

    private LayoutFormatter layout;

    @BeforeEach
    public void setUp() {
        layout = new HTMLChars();
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
                Arguments.of("&imath;", "\\{i}"),
                Arguments.of("&imath;&imath;", "\\i\\i"),
                Arguments.of("&auml;", "{\\\"{a}}"),
                Arguments.of("&auml;", "{\\\"a}"),
                Arguments.of("&auml;", "\\\"a"),
                Arguments.of("&Ccedil;", "{\\c{C}}"),
                Arguments.of("&Oogon;&imath;", "\\k{O}\\i"),
                Arguments.of("&ntilde; &ntilde; &iacute; &imath; &imath;", "\\~{n} \\~n \\'i \\i \\i")
        );
    }

    @ParameterizedTest
    @MethodSource("provideBasicFormattingData")
    void testBasicFormat(String expected, String input) {
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
                Arguments.of("<tt>hallo</tt>", "\\texttt{hallo}")
        );
    }

    @ParameterizedTest
    @MethodSource("provideLaTeXHighlightingFormattingData")
    void testLaTeXHighlighting(String expected, String input) {
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
    void testEquations(String expected, String input) {
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
    void testNewLine(String expected, String input) {
        assertEquals(expected, layout.format(input));
    }

    /*
     * Is missing a lot of test cases for the individual chars...
     */

    @Test
    void testQuoteSingle() {
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
                Arguments.of("&amp;HelloWorld", "&HelloWorld"),
                Arguments.of("&amp;amp;", "\\\\&"),
                Arguments.of("&lt;", "&lt;"),
                Arguments.of("&gt;", "&gt;")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHTMLEntityFormattingData")
    void testHTMLEntityFormatting(String expected, String input) {
        assertEquals(expected, layout.format(input));
    }
}
