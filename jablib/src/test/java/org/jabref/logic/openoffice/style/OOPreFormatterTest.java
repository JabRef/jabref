package org.jabref.logic.openoffice.style;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OOPreFormatterTest {

    @ParameterizedTest
    @CsvSource({
            // plain formatting
            "aaa, aaa",
            "\\$, $",
            "\\%, %",
            "\\\\, \\",

            // remove braces
            "{aaa}, aaa",

            // foreign accents
            "{\\\"{a}}, ä",
            "{\\\"{A}}, Ä",
            "{\\c{C}}, Ç",

            // special commands
            "{\\aa}, å",
            "{\\bb}, bb",
            "\\aa a, å a",
            "{\\aa a}, å a",
            "\\aa\\AA, åÅ",
            "\\bb a, bb a",

            // unsupported special commands
            "\\ftmch, ftmch",
            "{\\ftmch}, ftmch",
            "{\\ftmch\\aaa}, ftmchaaa",

            // equations
            "$\\Sigma$, Σ",

            // strip latex commands
            "\\mbox{-}, -",

            // formatting commands
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
    void format(String input, String expected) {
        assertEquals(expected, new OOPreFormatter().format(input));
    }
}
