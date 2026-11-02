package org.jabref.logic.formatter.bibtexfields;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->feat~save.cleanup.latex-to-unicode-aware-latex~1]
// [utest->req~save.cleanup.latex-to-unicode-aware-latex.keep-escaped-characters~1]
class LatexToUnicodeAwareLatexFormatterTest {

    private final LatexToUnicodeAwareLatexFormatter formatter = new LatexToUnicodeAwareLatexFormatter();

    @Test
    void formatExample() {
        assertEquals("Mönch, 50\\%", formatter.format(formatter.getExampleInput()));
    }

    @ParameterizedTest
    @MethodSource
    void convertsCharacterCommands(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    private static Stream<Arguments> convertsCharacterCommands() {
        return Stream.of(
                // Example from https://github.com/JabRef/jabref/issues/8715
                Arguments.of("Here is a © and it should be converted to Unicode", "Here is a \\textcopyright{} and it should be converted to Unicode"),
                Arguments.of("Mönch", "M\\\"onch"),
                Arguments.of("Mönch", "M\\\"{o}nch"),
                Arguments.of("Mönch", "M{\\\"o}nch"),
                Arguments.of("Mönch", "M{\\\"{o}}nch"),
                Arguments.of("é è ê ñ ā ż", "\\'e \\`e \\^e \\~n \\=a \\.z"),
                Arguments.of("ç ç š ő ą ğ å ṣ ṯ ṯ", "\\c{c} \\c c \\v{s} \\H{o} \\k{a} \\u{g} \\r{a} \\d{s} \\b{t} \\b t"),
                Arguments.of("í, ï, í", "\\'\\i, \\\"{\\i}, {\\'{\\i}}"),
                Arguments.of("Yuriĭ Dmitriĭ ǐ", "Yuri{\\u\\i} Dmitri\\u\\i{} \\v\\i"),
                Arguments.of("ß ø {Å} {œ} ł ı", "\\ss{} \\o{} {\\AA} {\\oe} \\l{} \\i{}"),
                Arguments.of("{Ö}sterreich", "{\\\"O}sterreich"),
                Arguments.of("§ ® ™ € – — …", "\\S{} \\textregistered{} \\texttrademark{} \\euro{} \\textendash{} \\textemdash{} \\ldots{}"),
                Arguments.of("© 2020 Müller", "\\textcopyright{} 2020 M\\\"uller"),
                Arguments.of("{Müller} and {BibTeX}", "{M\\\"uller} and {BibTeX}")
        );
    }

    @ParameterizedTest
    @MethodSource
    void swallowsSpacesAfterControlWords(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    private static Stream<Arguments> swallowsSpacesAfterControlWords() {
        return Stream.of(
                Arguments.of("Große Straße", "Gro\\ss e Stra{\\ss}e"),
                Arguments.of("naïve", "na\\\"\\i ve"),
                Arguments.of("ßX", "\\ss\nX"),
                // A blank line ends the paragraph
                Arguments.of("ß\n\nParagraph", "\\ss\n\nParagraph")
        );
    }

    @ParameterizedTest
    @MethodSource
    void keepsBracesWhereCharacterIsArgument(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    private static Stream<Arguments> keepsBracesWhereCharacterIsArgument() {
        return Stream.of(
                Arguments.of("\\textbf{ö}", "\\textbf{\\\"o}"),
                Arguments.of("\\textbf {ö}", "\\textbf {\\\"{o}}"),
                Arguments.of("\\emph{Ü}ber", "\\emph{\\\"U}ber"),
                Arguments.of("\\textbf{Mönch}", "\\textbf{M\\\"{o}nch}"),
                Arguments.of("\\ss{ö}", "\\ss{\\\"o}"),
                Arguments.of("\\^{é}", "\\^{\\'e}"),
                Arguments.of("{BibTeX}{ö}", "{BibTeX}{\\\"o}"),
                // Without braces, the command name would absorb the letter
                Arguments.of("\\LaTeX{ö} \\protect{ö}", "\\LaTeX\\\"o \\protect\\\"o"),
                Arguments.of("\\LaTeX ö", "\\LaTeX \\\"o"),
                Arguments.of("ßö", "\\ss\\\"o")
        );
    }

    @ParameterizedTest
    @MethodSource
    void keepsLatexMarkup(String input) {
        assertEquals(input, formatter.format(input));
    }

    private static Stream<String> keepsLatexMarkup() {
        return Stream.of(
                // Example from https://github.com/JabRef/jabref/issues/8715
                "Here is a backslashed percentage sign \\% and it should be excluded from conversion",
                "\\% \\& \\$ \\# \\_ \\{ \\}",
                "Line\\\\break and a control\\ space",
                "{BibTeX} and {DNA}",
                "\\textbf{bold} \\emph{word} \\textit{italic}",
                "H\\textsubscript{2}O and 1\\textsuperscript{st}",
                "$\\pi r^2$ and $\\alpha$",
                "$$\\sum_i x_i$$ \\(\\beta\\) \\[\\gamma\\]",
                "$\\\"{o}$ and \\begin{equation*}\\\"{o}\\end{equation*}",
                "\\begin{alignat}{2}\\\"{o}\\end{alignat}",
                "\\begin{cases}\\\"{o}\\end{cases}",
                """
                        % \\"{o} stays in the comment
                        """,
                "\\url{https://example.org/~M\\\"uller} \\cite[p.~1]{M\\\"uller} \\label{eq:\\\"o} \\verb|\\\"o|",
                "\\textbackslash{} \\textunderscore{} \\textless{} \\textasciitilde{}",
                "\\~{} \\^{} \\\"{}",
                "a\\v{}b x \\c{} y \\b",
                "\\aaaa{bbbb} \\LaTeX{} \\TeX",
                "~ -- --- ``quote''",
                "Price: \\$5 and \\$6",
                "unbalanced {brace",
                "plain text",
                ""
        );
    }

    @ParameterizedTest
    @MethodSource
    void convertsCharacterCommandsInMixedText(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    private static Stream<Arguments> convertsCharacterCommandsInMixedText() {
        return Stream.of(
                Arguments.of("Müller \\& Söhne, 50\\%", "M\\\"uller \\& S\\\"ohne, 50\\%"),
                Arguments.of("\\textbf{a \\% b} ©", "\\textbf{a \\% b} \\textcopyright{}"),
                Arguments.of("""
                        \\% ö % \\"o
                        ö
                        """, """
                        \\% \\"o % \\"o
                        \\"o
                        """),
                Arguments.of("Schrödinger: $\\\"{o}$ stays", "Schr\\\"odinger: $\\\"{o}$ stays"),
                Arguments.of("\\href{https://example.org/\\\"o}{Müller}", "\\href{https://example.org/\\\"o}{M\\\"uller}"),
                Arguments.of("\\\\ö", "\\\\\\\"o"),
                Arguments.of("\\{ö\\}", "\\{\\\"o\\}")
        );
    }

    @ParameterizedTest
    @MethodSource({"convertsCharacterCommands", "swallowsSpacesAfterControlWords", "keepsBracesWhereCharacterIsArgument", "convertsCharacterCommandsInMixedText"})
    void formattingTwiceChangesNothing(String expected, String input) {
        String formattedOnce = formatter.format(input);
        assertEquals(formattedOnce, formatter.format(formattedOnce));
    }
}
