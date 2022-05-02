package org.jabref.logic.openoffice.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OOPreFormatterTest {

    @Test
    public void testPlainFormat() {
        assertEquals("aaa", new OOPreFormatter().format("aaa"));
        assertEquals("$", new OOPreFormatter().format("\\$"));
        assertEquals("%", new OOPreFormatter().format("\\%"));
        assertEquals("\\", new OOPreFormatter().format("\\\\"));
    }

    @Test
    public void testFormatAccents() {
        assertEquals("ä", new OOPreFormatter().format("{\\\"{a}}"));
        assertEquals("Ä", new OOPreFormatter().format("{\\\"{A}}"));
        assertEquals("Ç", new OOPreFormatter().format("{\\c{C}}"));
    }

    @Test
    public void testSpecialCommands() {
        assertEquals("å", new OOPreFormatter().format("{\\aa}"));
        assertEquals("bb", new OOPreFormatter().format("{\\bb}"));
        assertEquals("å a", new OOPreFormatter().format("\\aa a"));
        assertEquals("å a", new OOPreFormatter().format("{\\aa a}"));
        assertEquals("åÅ", new OOPreFormatter().format("\\aa\\AA"));
        assertEquals("bb a", new OOPreFormatter().format("\\bb a"));
    }

    @Test
    public void testUnsupportedSpecialCommands() {
        assertEquals("ftmch", new OOPreFormatter().format("\\ftmch"));
        assertEquals("ftmch", new OOPreFormatter().format("{\\ftmch}"));
        assertEquals("ftmchaaa", new OOPreFormatter().format("{\\ftmch\\aaa}"));
    }

    @Test
    public void testEquations() {
        assertEquals("Σ", new OOPreFormatter().format("$\\Sigma$"));
    }

    @Test
    public void testFormatStripLatexCommands() {
        assertEquals("-", new OOPreFormatter().format("\\mbox{-}"));
    }

    @Test
    public void testFormatting() {
        assertEquals("<i>kkk</i>", new OOPreFormatter().format("\\textit{kkk}"));
        assertEquals("<i>kkk</i>", new OOPreFormatter().format("{\\it kkk}"));
        assertEquals("<i>kkk</i>", new OOPreFormatter().format("\\emph{kkk}"));
        assertEquals("<b>kkk</b>", new OOPreFormatter().format("\\textbf{kkk}"));
        assertEquals("<smallcaps>kkk</smallcaps>", new OOPreFormatter().format("\\textsc{kkk}"));
        assertEquals("<s>kkk</s>", new OOPreFormatter().format("\\sout{kkk}"));
        assertEquals("<u>kkk</u>", new OOPreFormatter().format("\\underline{kkk}"));
        assertEquals("<tt>kkk</tt>", new OOPreFormatter().format("\\texttt{kkk}"));
        assertEquals("<sup>kkk</sup>", new OOPreFormatter().format("\\textsuperscript{kkk}"));
        assertEquals("<sub>kkk</sub>", new OOPreFormatter().format("\\textsubscript{kkk}"));
    }
}
