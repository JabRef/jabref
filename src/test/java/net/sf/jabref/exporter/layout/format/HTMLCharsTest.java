package net.sf.jabref.exporter.layout.format;

import net.sf.jabref.exporter.layout.LayoutFormatter;

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

        Assert.assertEquals("&#305; &#305;", layout.format("\\i \\i"));
        Assert.assertEquals("&#305;", layout.format("\\i"));
        Assert.assertEquals("&#305;", layout.format("\\{i}"));
        Assert.assertEquals("&#305;&#305;", layout.format("\\i\\i"));

        Assert.assertEquals("&#319;&#305;", layout.format("\\Lmidot\\i"));

        Assert.assertEquals("&ntilde; &ntilde; &iacute; &#305; &#305;", layout.format("\\~{n} \\~n \\'i \\i \\i"));
    }

    @Test
    public void testLaTeXHighlighting() {

        LayoutFormatter layout = new HTMLChars();

        Assert.assertEquals("<em>hallo</em>", layout.format("\\emph{hallo}"));
        Assert.assertEquals("<em>hallo</em>", layout.format("{\\emph hallo}"));

        Assert.assertEquals("<em>hallo</em>", layout.format("\\textit{hallo}"));
        Assert.assertEquals("<em>hallo</em>", layout.format("{\\textit hallo}"));

        Assert.assertEquals("<b>hallo</b>", layout.format("\\textbf{hallo}"));
        Assert.assertEquals("<b>hallo</b>", layout.format("{\\textbf hallo}"));
    }

    /*
     * Is missing a lot of test cases for the individual chars...
     */
}