package org.jabref.logic.layout.format;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class OOPreFormatterTest {

    @ParameterizedTest
    @CsvSource(
            delimiterString = "->",
            textBlock = """
                    # plain formatting
                    aaa -> aaa
                    \\$ -> $
                    \\% -> %
                    \\\\ -> \\

                    # remove braces
                    {aaa} -> aaa

                    # foreign accents
                    {\\\"{a}} -> ä
                    {\\\"{A}} -> Ä
                    {\\c{C}} -> Ç

                    # special commands
                    {\\aa} -> å
                    {\\bb} -> bb
                    \\aa a -> å a
                    {\\aa a} -> å a
                    \\aa\\AA -> åÅ
                    \\bb a -> bb a

                    # unsupported special commands
                    \\ftmch -> ftmch
                    {\\ftmch} -> ftmch
                    {\\ftmch\\aaa} -> ftmchaaa

                    # equations
                    $\\Sigma$ -> Σ

                    # strip latex commands
                    \\mbox{-} -> -

                    # formatting commands
                    \\textit{kkk} -> <i>kkk</i>
                    {\\it kkk} -> <i>kkk</i>
                    \\emph{kkk} -> <i>kkk</i>
                    \\textbf{kkk} -> <b>kkk</b>
                    \\textsc{kkk} -> <smallcaps>kkk</smallcaps>
                    \\sout{kkk} -> <s>kkk</s>
                    \\underline{kkk} -> <u>kkk</u>
                    \\texttt{kkk} -> <tt>kkk</tt>
                    \\textsuperscript{kkk} -> <sup>kkk</sup>
                    \\textsubscript{kkk} -> <sub>kkk</sub>

                    # nested formatting and protection braces
                    \\textit{British \\textbf{Journal}ofNutrition} -> <i>British <b>Journal</b>ofNutrition</i>
                    \\textit{British \\textbf{Journal} of Nutrition} -> <i>British <b>Journal</b> of Nutrition</i>
                    \\textit{{British} \\textbf{{Journal}} of {Nutrition}} -> <i>British <b>Journal</b> of Nutrition</i>
                    {\\textit{{British}}} -> <i>British</i>
                    \\textbf{bold \\textit{italic \\underline{text}}} -> <b>bold <i>italic <u>text</u></i></b>

                    # escaped literal braces are not protection braces
                    \\{aaa\\} -> {aaa}
                    \\aa\\{aaa\\} -> å{aaa}
                    \\textit{a \\{literal\\} z} tail -> <i>a {literal} z</i> tail
                    \\textit{a \\{literal z} tail -> <i>a {literal z</i> tail
                    \\textit{a \\}literal z} tail -> <i>a }literal z</i> tail
                    \\textit{a \\{\\textbf{literal}\\} z} -> <i>a {<b>literal</b>} z</i>

                    # legacy formatting switches
                    {\\em British {\\bf Journal} of Nutrition} -> <i>British <b>Journal</b> of Nutrition</i>
                    {\\bf bold} -> <b>bold</b>
                    {\\sc Small caps} -> <smallcaps>Small caps</smallcaps>
                    {\\scshape Small caps} -> <smallcaps>Small caps</smallcaps>
                    \\textit{{\\sc Small} {\\scshape caps}} -> <i><smallcaps>Small</smallcaps> <smallcaps>caps</smallcaps></i>

                    # nested accents and normalization compatibility
                    \\textit{M{\\\"{u}}ller \\textbf{caf\\'{e}}} -> <i>Müller <b>café</b></i>
                    \\textit{cost \\textbf{\\$5} and $x$} -> <i>cost <b>$5</b> and x</i>
                    \\textit{A \\textbf{\\& B}} -> <i>A <b>&amp; B</b></i>
                    \\textit{\\textbf{\\$}x\\$} -> <i><b>$</b>x$</i>
                    <i>British <b>Journal</b> of Nutrition</i> -> <i>British <b>Journal</b> of Nutrition</i>
                    \\textit{British <b>Journal</b>} -> <i>British <b>Journal</b></i>
                    """
    )
    void format(String input, String expected) {
        assertEquals(expected, new OOPreFormatter().format(input));
    }
}
