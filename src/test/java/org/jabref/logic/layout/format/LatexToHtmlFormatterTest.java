package org.jabref.logic.layout.format;

import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatexToHtmlFormatterTest {

    final LatexToHtmlFormatter formatter = new LatexToHtmlFormatter();

    @ParameterizedTest(name = "{0}")
    // Non-working conversions were filed at https://github.com/davemckain/snuggletex/issues/7
    @CsvSource({
            "plainFormat, aaa, aaa",
            "formatUmlautLi, ä, {\\\"{a}}",
            "formatUmlautCa, Ä, {\\\"{A}}",
            // "formatUmlautLi, ı, \\i",
            // "formatUmlautCi, ı, {\\i}",
            "unknownCommandToSpan, '<span class=\"mbox\">-</span>', '\\mbox{-}'",
            "formatTextit, <i>text</i>, \\textit{text}",
            "escapedDollarSign, $, \\$",
            "curlyBracesAreRemoved, test, {test}",
            "curlyBracesAreRemovedInLongerText, a longer test there, a longer {test} there",
            "longConference, 'IEEE International Symposium on Circuits and Systems, ISCAS 2014, Melbourne, Victoria, Australia, June 1-5, 2014', '{IEEE} International Symposium on Circuits and Systems, {ISCAS} 2014, Melbourne, Victoria, Australia, June 1-5, 2014'",
            "longLatexedConferenceKeepsLatexCommands, 'in <em>IEEE International Symposium on Circuits and Systems, ISCAS 2014, Melbourne, Victoria, Australia, June 1-5, 2014.</em>', 'in \\emph{{IEEE} International Symposium on Circuits and Systems, {ISCAS} 2014, Melbourne, Victoria, Australia, June 1-5, 2014.}'",
            "formatExample, Mönch, Mönch",
            "iWithDiaresisAndUnnecessaryBraces, ï, {\\\"{i}}",
            "upperCaseIWithDiaresis, Ï, \\\"{I}",
            // "polishName, Łęski, \\L\\k{e}ski",
            // "doubleCombiningAccents, ώ, $\\acute{\\omega}$", // disabled, because not supported by SnuggleTeX yet - see https://github.com/davemckain/snuggletex/issues/5
            // "combiningAccentsCase1, ḩ, {\\c{h}}",
            // "ignoreUnknownCommandWithoutArgument, '', \\aaaa",
            // "ignoreUnknownCommandWithArgument, '', \\aaaa{bbbb}",
            // "removeUnknownCommandWithEmptyArgument, '', \\aaaa{}",
            // "sWithCaron, Š, {\\v{S}}",
            // "iWithDiaresisAndEscapedI, ı̈, \\\"{\\i}",
            "tildeN, Montaña, Monta\\~{n}a",
            // "acuteNLongVersion, Maliński, Mali\\'{n}ski",
            // "acuteNLongVersion, MaliŃski, Mali\\'{N}ski",
            // "acuteNShortVersion, Maliński, Mali\\'nski",
            // "acuteNShortVersion, MaliŃski, Mali\\'Nski",
            "apostrophN, Mali’nski, Mali'nski",
            "apostrophN, Mali’Nski, Mali'Nski",
            "apostrophO, L’oscillation, L'oscillation",
            "apostrophC, O’Connor, O'Connor",
            // (wrong LaTeX) "preservationOfSingleUnderscore, Lorem ipsum_lorem ipsum, Lorem ipsum_lorem ipsum",
            // (wrong LaTeX) "conversionOfUnderscoreWithBraces, Lorem ipsum_(lorem ipsum), Lorem ipsum_{lorem ipsum}",
            // "conversionOfOrdinal1st, 1ˢᵗ, 1\\textsuperscript{st}",
            // "conversionOfOrdinal2nd, 2ⁿᵈ, 2\\textsuperscript{nd}",
            // "conversionOfOrdinal3rd, 3ʳᵈ, 3\\textsuperscript{rd}",
            // "conversionOfOrdinal4th, 4ᵗʰ, 4\\textsuperscript{th}",
            // "conversionOfOrdinal9th, 9ᵗʰ, 9\\textsuperscript{th}",
            // "unicodeNames, 'Øie, Gunvor', '{\\O}ie, Gunvor'"
    })
    void formatterTest(String name, String expected, String input) {
        String htmlResult = formatter.format(input);
        String result = Jsoup.parse(htmlResult).body().html();
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"equationsSingleSymbol, σ, $\\sigma$",
            "equationsMoreComplicatedFormatting, A 32 mA ΣΔ -modulator, A 32~{mA} {$\\Sigma\\Delta$}-modulator",
            "equationsMoreComplicatedFormattingSigmaDeltaBraceVariant, Σ Δ, {\\(\\Sigma\\)}{\\(\\Delta\\)}",
            "equationsMoreComplicatedFormattingSigmaDeltaDollarVariant, Σ Δ, {{$\\Sigma$}}{{$\\Delta$}}",
            "longTitle, Linear programming design of semi-digital FIR filter and Σ Δ modulator for VDSL2 transmitter, Linear programming design of semi-digital {FIR} filter and {\\(\\Sigma\\)}{\\(\\Delta\\)} modulator for {VDSL2} transmitter",
            "chi, χ, $\\chi$",
            "iWithDiaresis, ï, \\\"{i}"
    })
    void math(String name, String expected, String input) {
        String htmlResult = formatter.format(input);
        String result = Jsoup.parse(htmlResult).body().text();
        assertEquals(expected, result);
    }
}
