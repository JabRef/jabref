package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class HTMLCharsTest {

    @Test
    public void testBasicFormat() {

        LayoutFormatter layout = new HTMLChars();

        Assert.assertEquals("", layout.format(""));

        Assert.assertEquals("hallo", layout.format("hallo"));

        Assert.assertEquals("Réflexions sur le timing de la quantité",
                layout.format("Réflexions sur le timing de la quantité"));

        Assert.assertEquals("h&aacute;llo", layout.format("h\\'allo"));

        Assert.assertEquals("&imath; &imath;", layout.format("\\i \\i"));
        Assert.assertEquals("&imath;", layout.format("\\i"));
        Assert.assertEquals("&imath;", layout.format("\\{i}"));
        Assert.assertEquals("&imath;&imath;", layout.format("\\i\\i"));

        Assert.assertEquals("&auml;", layout.format("{\\\"{a}}"));
        Assert.assertEquals("&auml;", layout.format("{\\\"a}"));
        Assert.assertEquals("&auml;", layout.format("\\\"a"));

        Assert.assertEquals("&Ccedil;", layout.format("{\\c{C}}"));

        Assert.assertEquals("&Lmidot;&imath;", layout.format("\\Lmidot\\i"));

        Assert.assertEquals("&ntilde; &ntilde; &iacute; &imath; &imath;", layout.format("\\~{n} \\~n \\'i \\i \\i"));
    }

    @Test
    public void testLaTeXHighlighting() {

        LayoutFormatter layout = new HTMLChars();

        Assert.assertEquals("<em>hallo</em>", layout.format("\\emph{hallo}"));
        Assert.assertEquals("<em>hallo</em>", layout.format("{\\emph hallo}"));
        Assert.assertEquals("<em>hallo</em>", layout.format("{\\em hallo}"));

        Assert.assertEquals("<i>hallo</i>", layout.format("\\textit{hallo}"));
        Assert.assertEquals("<i>hallo</i>", layout.format("{\\textit hallo}"));
        Assert.assertEquals("<i>hallo</i>", layout.format("{\\it hallo}"));

        Assert.assertEquals("<b>hallo</b>", layout.format("\\textbf{hallo}"));
        Assert.assertEquals("<b>hallo</b>", layout.format("{\\textbf hallo}"));
        Assert.assertEquals("<b>hallo</b>", layout.format("{\\bf hallo}"));

        Assert.assertEquals("<sup>hallo</sup>", layout.format("\\textsuperscript{hallo}"));
        Assert.assertEquals("<sub>hallo</sub>", layout.format("\\textsubscript{hallo}"));

        Assert.assertEquals("<u>hallo</u>", layout.format("\\underline{hallo}"));
        Assert.assertEquals("<s>hallo</s>", layout.format("\\sout{hallo}"));
        Assert.assertEquals("<tt>hallo</tt>", layout.format("\\texttt{hallo}"));

    }

    @Test
    public void testEquations() {
        LayoutFormatter layout = new HTMLChars();

        Assert.assertEquals("&dollar;", layout.format("\\$"));
        Assert.assertEquals("&sigma;", layout.format("$\\sigma$"));
        Assert.assertEquals("A 32&nbsp;mA &Sigma;&Delta;-modulator",
                layout.format("A 32~{mA} {$\\Sigma\\Delta$}-modulator"));
    }

    @Test
    public void testNewLine() {
        LayoutFormatter layout = new HTMLChars();
        Assert.assertEquals("a<br>b", layout.format("a\nb"));
        Assert.assertEquals("a<p>b", layout.format("a\n\nb"));
    }
    /*
     * Is missing a lot of test cases for the individual chars...
     */
}