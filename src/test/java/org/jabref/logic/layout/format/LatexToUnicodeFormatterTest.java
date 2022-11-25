package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatexToUnicodeFormatterTest {

    final LatexToUnicodeFormatter formatter = new LatexToUnicodeFormatter();

    @Test
    void testPlainFormat() {
        assertEquals("aaa", formatter.format("aaa"));
    }

    @Test
    void testFormatUmlaut() {
        assertEquals("ä", formatter.format("{\\\"{a}}"));
        assertEquals("Ä", formatter.format("{\\\"{A}}"));
    }

    @Test
    void preserveUnknownCommand() {
        assertEquals("\\mbox{-}", formatter.format("\\mbox{-}"));
    }

    @Test
    void testFormatTextit() {
        // See #1464
        assertEquals("\uD835\uDC61\uD835\uDC52\uD835\uDC65\uD835\uDC61", formatter.format("\\textit{text}"));
    }

    @Test
    void testEscapedDollarSign() {
        assertEquals("$", formatter.format("\\$"));
    }

    @Test
    void testEquationsSingleSymbol() {
        assertEquals("σ", formatter.format("$\\sigma$"));
    }

    @Test
    void testEquationsMoreComplicatedFormatting() {
        assertEquals("A 32 mA ΣΔ-modulator", formatter.format("A 32~{mA} {$\\Sigma\\Delta$}-modulator"));
    }

    @Test
    void formatExample() {
        assertEquals("Mönch", formatter.format(formatter.getExampleInput()));
    }

    @Test
    void testChi() {
        // See #1464
        assertEquals("χ", formatter.format("$\\chi$"));
    }

    @Test
    void testSWithCaron() {
        // Bug #1264
        assertEquals("Š", formatter.format("{\\v{S}}"));
    }

    @Test
    void testIWithDiaresis() {
        assertEquals("ï", formatter.format("\\\"{i}"));
    }

    @Test
    void testIWithDiaresisAndEscapedI() {
        // this might look strange in the test, but is actually a correct translation and renders identically to the above example in the UI
        assertEquals("ı̈", formatter.format("\\\"{\\i}"));
    }

    @Test
    void testIWithDiaresisAndUnnecessaryBraces() {
        assertEquals("ï", formatter.format("{\\\"{i}}"));
    }

    @Test
    void testUpperCaseIWithDiaresis() {
        assertEquals("Ï", formatter.format("\\\"{I}"));
    }

    @Test
    void testPolishName() {
        assertEquals("Łęski", formatter.format("\\L\\k{e}ski"));
    }

    @Test
    void testDoubleCombiningAccents() {
        assertEquals("ώ", formatter.format("$\\acute{\\omega}$"));
    }

    @Test
    void testCombiningAccentsCase1() {
        assertEquals("ḩ", formatter.format("{\\c{h}}"));
    }

    @Disabled("This is not a standard LaTeX command. It is debatable why we should convert this.")
    @Test
    void testCombiningAccentsCase2() {
        assertEquals("a͍", formatter.format("\\spreadlips{a}"));
    }

    @Test
    void keepUnknownCommandWithoutArgument() {
        assertEquals("\\aaaa", formatter.format("\\aaaa"));
    }

    @Test
    void keepUnknownCommandWithArgument() {
        assertEquals("\\aaaa{bbbb}", formatter.format("\\aaaa{bbbb}"));
    }

    @Test
    void keepUnknownCommandWithEmptyArgument() {
        assertEquals("\\aaaa{}", formatter.format("\\aaaa{}"));
    }

    @Test
    void testTildeN() {
        assertEquals("Montaña", formatter.format("Monta\\~{n}a"));
    }

    @Test
    void testAcuteNLongVersion() {
        assertEquals("Maliński", formatter.format("Mali\\'{n}ski"));
        assertEquals("MaliŃski", formatter.format("Mali\\'{N}ski"));
    }

    @Test
    void testAcuteNShortVersion() {
        assertEquals("Maliński", formatter.format("Mali\\'nski"));
        assertEquals("MaliŃski", formatter.format("Mali\\'Nski"));
    }

    @Test
    void testApostrophN() {
        assertEquals("Mali'nski", formatter.format("Mali'nski"));
        assertEquals("Mali'Nski", formatter.format("Mali'Nski"));
    }

    @Test
    void testApostrophO() {
        assertEquals("L'oscillation", formatter.format("L'oscillation"));
    }

    @Test
    void testApostrophC() {
        assertEquals("O'Connor", formatter.format("O'Connor"));
    }

    @Test
    void testPreservationOfSingleUnderscore() {
        assertEquals("Lorem ipsum_lorem ipsum", formatter.format("Lorem ipsum_lorem ipsum"));
    }

    @Test
    void testConversionOfUnderscoreWithBraces() {
        assertEquals("Lorem ipsum_(lorem ipsum)", formatter.format("Lorem ipsum_{lorem ipsum}"));
    }

    @Test
    void testConversionOfOrdinal1st() {
        assertEquals("1ˢᵗ", formatter.format("1\\textsuperscript{st}"));
    }

    @Test
    void testConversionOfOrdinal2nd() {
        assertEquals("2ⁿᵈ", formatter.format("2\\textsuperscript{nd}"));
    }

    @Test
    void testConversionOfOrdinal3rd() {
        assertEquals("3ʳᵈ", formatter.format("3\\textsuperscript{rd}"));
    }

    @Test
    void testConversionOfOrdinal4th() {
        assertEquals("4ᵗʰ", formatter.format("4\\textsuperscript{th}"));
    }

    @Test
    void testConversionOfOrdinal9th() {
        assertEquals("9ᵗʰ", formatter.format("9\\textsuperscript{th}"));
    }
}
