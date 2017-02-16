package org.jabref.logic.layout.format;

import org.junit.Ignore;
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
        assertEquals("A 32 mA ΣΔ-modulator", formatter.format("A 32~{mA} {$\\Sigma\\Delta$}-modulator"));
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
    public void testIWithDiaresis() {
        assertEquals("ï", formatter.format("\\\"{i}"));
    }

    @Test
    public void testIWithDiaresisAndEscapedI() {
        // this might look strange in the test, but is actually a correct translation and renders identically to the above example in the UI
        assertEquals("ı̈", formatter.format("\\\"{\\i}"));
    }


    @Test
    public void testIWithDiaresisAndUnnecessaryBraces() {
        assertEquals("ï", formatter.format("{\\\"{i}}"));
    }

    @Test
    public void testUpperCaseIWithDiaresis() {
        assertEquals("Ï", formatter.format("\\\"{I}"));
    }

    @Test
    public void testPolishName() {
        assertEquals("Łęski", formatter.format("\\L\\k{e}ski"));
    }


    @Test
    public void testDoubleCombiningAccents() {
        assertEquals("ώ", formatter.format("$\\acute{\\omega}$"));
    }

    @Test
    public void testCombiningAccentsCase1() {
        assertEquals("ḩ", formatter.format("{\\c{h}}"));
    }

    @Ignore("This is not a standard LaTeX command. It is debatable why we should convert this.")
    @Test
    public void testCombiningAccentsCase2() {
        assertEquals("a͍", formatter.format("\\spreadlips{a}"));
    }

    @Test
    public void unknownCommandIsIgnored() {
        assertEquals("", formatter.format("\\aaaa"));
    }

    @Test
    public void unknownCommandKeepsArgument() {
        assertEquals("bbbb", formatter.format("\\aaaa{bbbb}"));
    }

    @Test
    public void unknownCommandWithEmptyArgumentIsIgnored() {
        assertEquals("", formatter.format("\\aaaa{}"));
    }

    @Test
    public void testTildeN() {
        assertEquals("Montaña", formatter.format("Monta\\~{n}a"));
    }

    @Test
    public void testAcuteNLongVersion() {
        assertEquals("Maliński", formatter.format("Mali\\'{n}ski"));
        assertEquals("MaliŃski", formatter.format("Mali\\'{N}ski"));
    }

    @Test
    public void testAcuteNShortVersion() {
        assertEquals("Maliński", formatter.format("Mali\\'nski"));
        assertEquals("MaliŃski", formatter.format("Mali\\'Nski"));
    }

    @Test
    public void testApostrophN() {
        assertEquals("Mali'nski", formatter.format("Mali'nski"));
        assertEquals("Mali'Nski", formatter.format("Mali'Nski"));
    }

    @Test
    public void testApostrophO() {
        assertEquals("L'oscillation", formatter.format("L'oscillation"));
    }

    @Test
    public void testApostrophC() {
        assertEquals("O'Connor", formatter.format("O'Connor"));
    }
}
