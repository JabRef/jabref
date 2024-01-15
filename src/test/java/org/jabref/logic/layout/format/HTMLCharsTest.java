package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HTMLCharsTest {

    private LayoutFormatter layout;

    @BeforeEach
    void setUp() {
        layout = new HTMLChars();
    }

    @Test
    void basicFormat() {
        assertEquals("", layout.format(""));

        assertEquals("hallo", layout.format("hallo"));

        assertEquals("Réflexions sur le timing de la quantité",
                layout.format("Réflexions sur le timing de la quantité"));

        assertEquals("%%%", layout.format("\\%\\%\\%"));
        assertEquals("People remember 10%, 20%…Oh Really?", layout.format("{{People remember 10\\%, 20\\%…Oh Really?}}"));

        assertEquals("h&aacute;llo", layout.format("h\\'allo"));

        assertEquals("&imath; &imath;", layout.format("\\i \\i"));
        assertEquals("&imath;", layout.format("\\i"));
        assertEquals("&imath;", layout.format("\\{i}"));
        assertEquals("&imath;&imath;", layout.format("\\i\\i"));

        assertEquals("&auml;", layout.format("{\\\"{a}}"));
        assertEquals("&auml;", layout.format("{\\\"a}"));
        assertEquals("&auml;", layout.format("\\\"a"));

        assertEquals("&Ccedil;", layout.format("{\\c{C}}"));

        assertEquals("&Oogon;&imath;", layout.format("\\k{O}\\i"));

        assertEquals("&ntilde; &ntilde; &iacute; &imath; &imath;", layout.format("\\~{n} \\~n \\'i \\i \\i"));
    }

    @Test
    void laTeXHighlighting() {
        assertEquals("<em>hallo</em>", layout.format("\\emph{hallo}"));
        assertEquals("<em>hallo</em>", layout.format("{\\emph hallo}"));
        assertEquals("<em>hallo</em>", layout.format("{\\em hallo}"));

        assertEquals("<i>hallo</i>", layout.format("\\textit{hallo}"));
        assertEquals("<i>hallo</i>", layout.format("{\\textit hallo}"));
        assertEquals("<i>hallo</i>", layout.format("{\\it hallo}"));

        assertEquals("<b>hallo</b>", layout.format("\\textbf{hallo}"));
        assertEquals("<b>hallo</b>", layout.format("{\\textbf hallo}"));
        assertEquals("<b>hallo</b>", layout.format("{\\bf hallo}"));

        assertEquals("<sup>hallo</sup>", layout.format("\\textsuperscript{hallo}"));
        assertEquals("<sub>hallo</sub>", layout.format("\\textsubscript{hallo}"));

        assertEquals("<u>hallo</u>", layout.format("\\underline{hallo}"));
        assertEquals("<s>hallo</s>", layout.format("\\sout{hallo}"));
        assertEquals("<tt>hallo</tt>", layout.format("\\texttt{hallo}"));
    }

    @Test
    void equations() {
        assertEquals("&dollar;", layout.format("\\$"));
        assertEquals("&sigma;", layout.format("$\\sigma$"));
        assertEquals("A 32&nbsp;mA &Sigma;&Delta;-modulator",
                layout.format("A 32~{mA} {$\\Sigma\\Delta$}-modulator"));
    }

    @Test
    void newLine() {
        assertEquals("a<br>b", layout.format("a\nb"));
        assertEquals("a<p>b", layout.format("a\n\nb"));
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
}
