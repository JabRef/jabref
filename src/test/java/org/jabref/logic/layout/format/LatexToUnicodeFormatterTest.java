package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatexToUnicodeFormatterTest {

    final LatexToUnicodeFormatter formatter = new LatexToUnicodeFormatter();

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "plainFormat, aaa, aaa",
            "formatUmlautLi, ä, {\\\"{a}}",
            "formatUmlautCa, Ä, {\\\"{A}}",
            "formatUmlautLi, ı, \\i",
            "formatUmlautCi, ı, {\\i}",
            "preserveUnknownCommand, '\\mbox{-}', '\\mbox{-}'",
            "formatTextit, \uD835\uDC61\uD835\uDC52\uD835\uDC65\uD835\uDC61, \\textit{text}",
            "escapedDollarSign, $, \\$",
            "equationsSingleSymbol, σ, $\\sigma$",
            "curlyBracesAreRemoved, test, {test}",
            "curlyBracesAreRemovedInLongerText, a longer test there, a longer {test} there",
            "equationsMoreComplicatedFormatting, A 32 mA ΣΔ-modulator, A 32~{mA} {$\\Sigma\\Delta$}-modulator",
            "equationsMoreComplicatedFormattingSigmaDeltaBraceVariant, ΣΔ, {\\(\\Sigma\\)}{\\(\\Delta\\)}",
            "equationsMoreComplicatedFormattingSigmaDeltaDollarVariant, ΣΔ, {{$\\Sigma$}}{{$\\Delta$}}",
            "longTitle, Linear programming design of semi-digital FIR filter and ΣΔ modulator for VDSL2 transmitter, Linear programming design of semi-digital {FIR} filter and {\\(\\Sigma\\)}{\\(\\Delta\\)} modulator for {VDSL2} transmitter",
            "longConference, 'IEEE International Symposium on Circuits and Systems, ISCAS 2014, Melbourne, Victoria, Australia, June 1-5, 2014', '{IEEE} International Symposium on Circuits and Systems, {ISCAS} 2014, Melbourne, Victoria, Australia, June 1-5, 2014'",
            "longLatexedConferenceKeepsLatexCommands, 'in \\emph{{IEEE} International Symposium on Circuits and Systems, {ISCAS} 2014, Melbourne, Victoria, Australia, June 1-5, 2014.}', 'in \\emph{{IEEE} International Symposium on Circuits and Systems, {ISCAS} 2014, Melbourne, Victoria, Australia, June 1-5, 2014.}'",
            "formatExample, Mönch, Mönch",
            "chi, χ, $\\chi$",
            "sWithCaron, Š, {\\v{S}}",
            "iWithDiaresis, ï, \\\"{i}",
            "iWithDiaresisAndEscapedI, ı̈, \\\"{\\i}",
            "iWithDiaresisAndUnnecessaryBraces, ï, {\\\"{i}}",
            "upperCaseIWithDiaresis, Ï, \\\"{I}",
            "polishName, Łęski, \\L\\k{e}ski",
            "doubleCombiningAccents, ώ, $\\acute{\\omega}$",
            "combiningAccentsCase1, ḩ, {\\c{h}}",
            "keepUnknownCommandWithoutArgument, \\aaaa, \\aaaa",
            "keepUnknownCommandWithArgument, \\aaaa{bbbb}, \\aaaa{bbbb}",
            "keepUnknownCommandWithEmptyArgument, \\aaaa{}, \\aaaa{}",
            "tildeN, Montaña, Monta\\~{n}a",
            "acuteNLongVersion, Maliński, Mali\\'{n}ski",
            "acuteNLongVersion, MaliŃski, Mali\\'{N}ski",
            "acuteNShortVersion, Maliński, Mali\\'nski",
            "acuteNShortVersion, MaliŃski, Mali\\'Nski",
            "apostrophN, Mali'nski, Mali'nski",
            "apostrophN, Mali'Nski, Mali'Nski",
            "apostrophO, L'oscillation, L'oscillation",
            "apostrophC, O'Connor, O'Connor",
            "preservationOfSingleUnderscore, Lorem ipsum_lorem ipsum, Lorem ipsum_lorem ipsum",
            "conversionOfUnderscoreWithBraces, Lorem ipsum_(lorem ipsum), Lorem ipsum_{lorem ipsum}",
            "conversionOfOrdinal1st, 1ˢᵗ, 1\\textsuperscript{st}",
            "conversionOfOrdinal2nd, 2ⁿᵈ, 2\\textsuperscript{nd}",
            "conversionOfOrdinal3rd, 3ʳᵈ, 3\\textsuperscript{rd}",
            "conversionOfOrdinal4th, 4ᵗʰ, 4\\textsuperscript{th}",
            "conversionOfOrdinal9th, 9ᵗʰ, 9\\textsuperscript{th}",
            "unicodeNames, 'Øie, Gunvor', '{\\O}ie, Gunvor'"
    })
    void formatterTest(String name, String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @Disabled("This is not a standard LaTeX command. It is debatable why we should convert this.")
    @Test
    void combiningAccentsCase2() {
        assertEquals("a͍", formatter.format("\\spreadlips{a}"));
    }
}
