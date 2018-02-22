package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals("\uD835\uDC61\uD835\uDC52\uD835\uDC65\uD835\uDC61", formatter.format("\\textit{text}"));
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

    @Disabled("This is not a standard LaTeX command. It is debatable why we should convert this.")
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

    @Test
    public void testPreservationOfSingleUnderscore() {
        assertEquals("Lorem ipsum_lorem ipsum", formatter.format("Lorem ipsum_lorem ipsum"));
    }

    @Test
    public void testConversionOfUnderscoreWithBraces() {
        assertEquals("Lorem ipsum_(lorem ipsum)", formatter.format("Lorem ipsum_{lorem ipsum}"));
    }

    @Test
    public void testConversionOfOrdinal1st() {
        assertEquals("1ˢᵗ", formatter.format("1\\textsuperscript{st}"));
    }

    @Test
    public void testConversionOfOrdinal2nd() {
        assertEquals("2ⁿᵈ", formatter.format("2\\textsuperscript{nd}"));
    }

    @Test
    public void testConversionOfOrdinal3rd() {
        assertEquals("3ʳᵈ", formatter.format("3\\textsuperscript{rd}"));
    }

    @Test
    public void testConversionOfOrdinal4th() {
        assertEquals("4ᵗʰ", formatter.format("4\\textsuperscript{th}"));
    }

    @Test
    public void testConversionOfOrdinal9th() {
        assertEquals("9ᵗʰ", formatter.format("9\\textsuperscript{th}"));
    }

}
