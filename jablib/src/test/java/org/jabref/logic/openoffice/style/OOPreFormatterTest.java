package org.jabref.logic.openoffice.style;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OOPreFormatterTest {

    @ParameterizedTest
    @CsvSource({
        "aaa, aaa",
        "\\$, $",
        "\\%, %",
        "\\\\, \\"
    })
    void plainFormat(String input, String expected) {
        assertEquals(expected, new OOPreFormatter().format(input));
    }

    @Test
    void removeBraces() {
        assertEquals("aaa", new OOPreFormatter().format("{aaa}"));
    }

    @ParameterizedTest
    @CsvSource({
        "{\\\"{a}}, ä",
        "{\\\"{A}}, Ä",
        "{\\c{C}}, Ç"
    })
    void formatAccents(String input, String expected) {
        assertEquals(expected, new OOPreFormatter().format(input));
    }

    @ParameterizedTest
    @CsvSource({
        "{\\aa}, å",
        "{\\bb}, bb",
        "\\aa a, å a",
        "{\\aa a}, å a",
        "\\aa\\AA, åÅ",
        "\\bb a, bb a"
    })
    void specialCommands(String input, String expected) {
        assertEquals(expected, new OOPreFormatter().format(input));
    }

    @ParameterizedTest
    @CsvSource({
        "\\ftmch, ftmch",
        "{\\ftmch}, ftmch",
        "{\\ftmch\\aaa}, ftmchaaa"
    })
    void unsupportedSpecialCommands(String input, String expected) {
        assertEquals(expected, new OOPreFormatter().format(input));
    }

    @Test
    void equations() {
        assertEquals("Σ", new OOPreFormatter().format("$\\Sigma$"));
    }

    @Test
    void formatStripLatexCommands() {
        assertEquals("-", new OOPreFormatter().format("\\mbox{-}"));
    }

    @ParameterizedTest
    @CsvSource({
        "\\textit{kkk}, <i>kkk</i>",
        "{\\it kkk}, <i>kkk</i>",
        "\\emph{kkk}, <i>kkk</i>",
        "\\textbf{kkk}, <b>kkk</b>",
        "\\textsc{kkk}, <smallcaps>kkk</smallcaps>",
        "\\sout{kkk}, <s>kkk</s>",
        "\\underline{kkk}, <u>kkk</u>",
        "\\texttt{kkk}, <tt>kkk</tt>",
        "\\textsuperscript{kkk}, <sup>kkk</sup>",
        "\\textsubscript{kkk}, <sub>kkk</sub>"
    })
    void formatting(String input, String expected) {
        assertEquals(expected, new OOPreFormatter().format(input));
    }
}
