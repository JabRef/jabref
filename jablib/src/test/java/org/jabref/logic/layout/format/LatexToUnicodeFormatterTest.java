package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatexToUnicodeFormatterTest {

    final LatexToUnicodeFormatter formatter = new LatexToUnicodeFormatter();

    @Test
    void plainFormat() {
        assertEquals("aaa", formatter.format("aaa"));
    }

    @Test
    void formatUmlaut() {
        assertEquals("ä", formatter.format("{\\\"{a}}"));
        assertEquals("Ä", formatter.format("{\\\"{A}}"));
    }

    @ParameterizedTest
    @CsvSource({
            "ı, \\i",
            "ı, {\\i}"
    })
    void smallIwithoutDot(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @Test
    void preserveUnknownCommand() {
        assertEquals("\\mbox{-}", formatter.format("\\mbox{-}"));
    }

    @Test
    void formatTextit() {
        // See #1464
        assertEquals("\uD835\uDC61\uD835\uDC52\uD835\uDC65\uD835\uDC61", formatter.format("\\textit{text}"));
    }

    @Test
    void escapedDollarSign() {
        assertEquals("$", formatter.format("\\$"));
    }

    @Test
    void equationsSingleSymbol() {
        assertEquals("σ", formatter.format("$\\sigma$"));
    }

    @Test
    void equationsMoreComplicatedFormatting() {
        assertEquals("A 32\u00a0mA ΣΔ-modulator", formatter.format("A 32~{mA} {$\\Sigma\\Delta$}-modulator"));
    }

    @Test
    void equationsWithNestedScriptsRemainReadable() {
        assertEquals("Much progress has been made in classifying when the weak Lefschetz property holds for A=𝔽[x,y,z]/I where char(𝔽)=0 and I=(xᵈ₁,yᵈ₂,zᵈ₃,xᵃ₁yᵃ₂zᵃ₃)",
                formatter.format("Much progress has been made in classifying when the weak Lefschetz property holds for $A=\\mathbb{F}[x,y,z]/I$ where $\\text{char}(\\mathbb{F})=0$ and $I=(x^{d_{1}},y^{d_{2}},z^{d_{3}},x^{a_{1}}y^{a_{2}}z^{a_{3}})$"));
    }

    @Test
    void equationsWithTextLikeMathCommandsRemainReadable() {
        assertEquals("Let R be a standard graded algebra over an infinite field k, and let 𝔹_ℚ(R) and 𝔹_ℚᵖᵘʳᵉ(R) denote the rational cones spanned by the Betti tables of all finitely generated R-modules and of those with pure resolutions, respectively. We establish several necessary conditions for the equality 𝔹_ℚ(R) = 𝔹_ℚᵖᵘʳᵉ(R). When edim(R)≥ 2, we prove that k has a pure resolution if and only if it has a linear resolution, and consequently, if the extremal rays of 𝔹_ℚ(R) are pure, then R is Koszul and good (in the sense of Roos). We show that if R has depth zero, it must be Artinian for the equality of the two cones to hold. For rings with linear pairs of exact zerodivisors, we show that the equality of the cones implies that the h-polynomial has degree at most 2, and use it to characterize generic Gorenstein Artin algebras satisfying 𝔹_ℚ(R) = 𝔹_ℚᵖᵘʳᵉ(R). We also characterize algebras whose extremal rays are exactly the Betti tables of shifts of R/𝔪ʲ and of pure modules M with codim(M)=pdim(M): apart from polynomial rings, these are precisely Cohen–Macaulay algebras of dimension at most one with minimal multiplicity. In addition, we obtain a characterization of Cohen–Macaulay algebras of minimal multiplicity in terms of the extremal rays of the Betti cone of maximal Cohen–Macaulay modules.",
                formatter.format("Let $R$ be a standard graded algebra over an infinite field $\\mathsf k$, and let $\\mathbb{B}_{\\mathbb{Q}}(R)$ and $\\mathbb{B}_{\\mathbb{Q}}^{\\mathrm{pure}}(R)$ denote the rational cones spanned by the Betti tables of all finitely generated $R$-modules and of those with pure resolutions, respectively. We establish several necessary conditions for the equality $\\mathbb{B}_{\\mathbb{Q}}(R) = \\mathbb{B}_{\\mathbb{Q}}^{\\mathrm{pure}}(R)$. When $\\operatorname{edim}(R)\\ge 2$, we prove that $\\mathsf k$ has a pure resolution if and only if it has a linear resolution, and consequently, if the extremal rays of $\\mathbb{B}_{\\mathbb{Q}}(R)$ are pure, then $R$ is Koszul and good (in the sense of Roos). We show that if $R$ has depth zero, it must be Artinian for the equality of the two cones to hold. For rings with linear pairs of exact zerodivisors, we show that the equality of the cones implies that the $h$-polynomial has degree at most $2$, and use it to characterize generic Gorenstein Artin algebras satisfying $\\mathbb{B}_{\\mathbb{Q}}(R) = \\mathbb{B}_{\\mathbb{Q}}^{\\mathrm{pure}}(R)$. We also characterize algebras whose extremal rays are exactly the Betti tables of shifts of $R/\\mathfrak m^j$ and of pure modules $M$ with $\\operatorname{codim}(M)=\\operatorname{pdim}(M)$: apart from polynomial rings, these are precisely Cohen--Macaulay algebras of dimension at most one with minimal multiplicity. In addition, we obtain a characterization of Cohen--Macaulay algebras of minimal multiplicity in terms of the extremal rays of the Betti cone of maximal Cohen--Macaulay modules."));
    }

    @Test
    void formatExample() {
        assertEquals("Mönch", formatter.format(formatter.getExampleInput()));
    }

    @Test
    void chi() {
        // See #1464
        assertEquals("χ", formatter.format("$\\chi$"));
    }

    @Test
    void sWithCaron() {
        // Bug #1264
        assertEquals("Š", formatter.format("{\\v{S}}"));
    }

    @Test
    void iWithDiaresis() {
        assertEquals("ï", formatter.format("\\\"{i}"));
    }

    @Test
    void iWithDiaresisAndEscapedI() {
        // this might look strange in the test, but is actually a correct translation and renders identically to the above example in the UI
        assertEquals("ı̈", formatter.format("\\\"{\\i}"));
    }

    @Test
    void iWithDiaresisAndUnnecessaryBraces() {
        assertEquals("ï", formatter.format("{\\\"{i}}"));
    }

    @Test
    void upperCaseIWithDiaresis() {
        assertEquals("Ï", formatter.format("\\\"{I}"));
    }

    @Test
    void polishName() {
        assertEquals("Łęski", formatter.format("\\L\\k{e}ski"));
    }

    @Test
    void doubleCombiningAccents() {
        assertEquals("ώ", formatter.format("$\\acute{\\omega}$"));
    }

    @Test
    void combiningAccentsCase1() {
        assertEquals("ḩ", formatter.format("{\\c{h}}"));
    }

    @Disabled("This is not a standard LaTeX command. It is debatable why we should convert this.")
    @Test
    void combiningAccentsCase2() {
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
    void tildeN() {
        assertEquals("Montaña", formatter.format("Monta\\~{n}a"));
    }

    @Test
    void acuteNLongVersion() {
        assertEquals("Maliński", formatter.format("Mali\\'{n}ski"));
        assertEquals("MaliŃski", formatter.format("Mali\\'{N}ski"));
    }

    @Test
    void acuteNShortVersion() {
        assertEquals("Maliński", formatter.format("Mali\\'nski"));
        assertEquals("MaliŃski", formatter.format("Mali\\'Nski"));
    }

    @Test
    void apostrophN() {
        assertEquals("Mali'nski", formatter.format("Mali'nski"));
        assertEquals("Mali'Nski", formatter.format("Mali'Nski"));
    }

    @Test
    void apostrophO() {
        assertEquals("L'oscillation", formatter.format("L'oscillation"));
    }

    @Test
    void apostrophC() {
        assertEquals("O'Connor", formatter.format("O'Connor"));
    }

    @Test
    void preservationOfSingleUnderscore() {
        assertEquals("Lorem ipsum_lorem ipsum", formatter.format("Lorem ipsum_lorem ipsum"));
    }

    @Test
    void conversionOfUnderscoreWithBraces() {
        assertEquals("Lorem ipsum_(lorem ipsum)", formatter.format("Lorem ipsum_{lorem ipsum}"));
    }

    /// <a href="https://github.com/JabRef/jabref/issues/5547">Issue 5547</a>
    @Test
    void twoDifferentMacrons() {
        assertEquals("Puṇya-pattana-vidyā-pı̄ṭhādhi-kṛtaiḥ prā-kaśyaṃ nı̄taḥ", formatter.format("Pu{\\d{n}}ya-pattana-vidy{\\={a}}-p{\\={\\i}}{\\d{t}}h{\\={a}}dhi-k{\\d{r}}tai{\\d{h}} pr{\\={a}}-ka{{\\'{s}}}ya{\\d{m}} n{\\={\\i}}ta{\\d{h}}"));
    }

    @Test
    void conversionOfOrdinal1st() {
        assertEquals("1ˢᵗ", formatter.format("1\\textsuperscript{st}"));
    }

    @Test
    void conversionOfOrdinal2nd() {
        assertEquals("2ⁿᵈ", formatter.format("2\\textsuperscript{nd}"));
    }

    @Test
    void conversionOfOrdinal3rd() {
        assertEquals("3ʳᵈ", formatter.format("3\\textsuperscript{rd}"));
    }

    @Test
    void conversionOfOrdinal4th() {
        assertEquals("4ᵗʰ", formatter.format("4\\textsuperscript{th}"));
    }

    @Test
    void conversionOfOrdinal9th() {
        assertEquals("9ᵗʰ", formatter.format("9\\textsuperscript{th}"));
    }

    @Test
    void formatPreservesNoBreakSpaces() {
        assertEquals("Y.\u00a0Matsumoto", formatter.format("Y.~Matsumoto"));
    }
}
