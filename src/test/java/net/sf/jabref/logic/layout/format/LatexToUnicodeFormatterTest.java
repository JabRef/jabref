package net.sf.jabref.logic.layout.format;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class LatexToUnicodeFormatterTest {

    public final LatexToUnicodeFormatter formatter = new LatexToUnicodeFormatter();

    @Test
    public void testPlainFormat() {
        assertEquals("aaa", formatter.format("aaa"));
    }

    @Test
    public void testFormatUmlaut() {
        assertEquals("ä", formatter.format("{\\\"{a}}"));
        assertEquals("Ä", formatter.format("{\\\"{A}}"));
    }

    @Test
    public void testFormatStripLatexCommands() {
        assertEquals("-", formatter.format("\\mbox{-}"));
    }

    @Test
    public void testFormatTextit() {
        // See #1464
        assertEquals("text", formatter.format("\\textit{text}"));
    }

    @Test
    public void testEscapedDollarSign() {
        assertEquals("$", formatter.format("\\$"));
    }

    @Test
    public void testEquationsSingleSymbol() {
        assertEquals("σ", formatter.format("$\\sigma$"));
    }

    @Test
    public void testEquationsMoreComplicatedFormatting() {
        assertEquals("A 32\u00A0mA ΣΔ-modulator", formatter.format("A 32~{mA} {$\\Sigma\\Delta$}-modulator"));
    }

    @Test
    public void formatExample() {
        assertEquals("Mönch", formatter.format(formatter.getExampleInput()));
    }

    @Test
    public void testChi() {
        // See #1464
        assertEquals("χ", formatter.format("$\\chi$"));
    }

    @Test
    public void testSWithCaron() {
        // Bug #1264
        assertEquals("Š", formatter.format("{\\v{S}}"));
    }

    @Test
    public void testCombiningAccentsCase1() {
        assertEquals("ḩ", formatter.format("{\\c{h}}"));
    }

    @Test
    public void testCombiningAccentsCase2() {
        assertEquals("a͍", formatter.format("\\spreadlips{a}"));
    }

    @Test
    public void unknownCommandIsKept() {
        assertEquals("aaaa", formatter.format("\\aaaa"));
    }

    @Test
    public void unknownCommandKeepsArgument() {
        assertEquals("bbbb", formatter.format("\\aaaa{bbbb}"));
    }

    @Test
    public void unknownCommandWithEmptyArgumentIsKept() {
        assertEquals("aaaa", formatter.format("\\aaaa{}"));
    }

    @Test
    public void testTildeN () {
        assertEquals("Montaña", formatter.format("Monta\\~{n}a"));
    }

    @Test
    public void testApostrophN () {
        assertEquals("Maliński", formatter.format("Mali\\'{n}ski"));
        assertEquals("Maliŉski", formatter.format("Mali'nski"));
    }
}
