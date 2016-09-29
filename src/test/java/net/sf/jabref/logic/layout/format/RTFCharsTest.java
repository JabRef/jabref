package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RTFCharsTest {
    private LayoutFormatter formatter;

    @Before
    public void setUp() {
        formatter = new RTFChars();
    }

    @After
    public void tearDown() {
        formatter = null;
    }

    @Test
    public void testBasicFormat() {
        Assert.assertEquals("", formatter.format(""));

        Assert.assertEquals("hallo", formatter.format("hallo"));

        Assert.assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e",
                formatter.format("Réflexions sur le timing de la quantité"));

        Assert.assertEquals("h\\u225allo", formatter.format("h\\'allo"));
        Assert.assertEquals("h\\u225allo", formatter.format("h\\'allo"));
    }

    @Test
    public void testLaTeXHighlighting() {
        Assert.assertEquals("{\\i hallo}", formatter.format("\\emph{hallo}"));
        Assert.assertEquals("{\\i hallo}", formatter.format("{\\emph hallo}"));
        Assert.assertEquals("An article title with {\\i a book title} emphasized", formatter.format("An article title with \\emph{a book title} emphasized"));

        Assert.assertEquals("{\\i hallo}", formatter.format("\\textit{hallo}"));
        Assert.assertEquals("{\\i hallo}", formatter.format("{\\textit hallo}"));

        Assert.assertEquals("{\\b hallo}", formatter.format("\\textbf{hallo}"));
        Assert.assertEquals("{\\b hallo}", formatter.format("{\\textbf hallo}"));
    }

    @Test
    public void testComplicated() {
        Assert.assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e {\\u230ae} should be \\u230ae",
                formatter.format("Réflexions sur le timing de la quantité {\\ae} should be æ"));

        Assert.assertEquals("h\\u225all{\\u339oe}", formatter.format("h\\'all{\\oe}"));
    }

    @Test
    public void testSpecialCharacters() {
        Assert.assertEquals("\\u243o", formatter.format("\\'{o}")); // ó
        Assert.assertEquals("\\'f2", formatter.format("\\`{o}")); // ò
        Assert.assertEquals("\\'f4", formatter.format("\\^{o}")); // ô
        Assert.assertEquals("\\'f6", formatter.format("\\\"{o}")); // ö
        Assert.assertEquals("\\u245o", formatter.format("\\~{o}")); // õ
        Assert.assertEquals("\\u333o", formatter.format("\\={o}"));
        Assert.assertEquals("\\u335o", formatter.format("{\\uo}"));
        Assert.assertEquals("\\u231c", formatter.format("{\\cc}")); // ç
        Assert.assertEquals("{\\u339oe}", formatter.format("{\\oe}"));
        Assert.assertEquals("{\\u338OE}", formatter.format("{\\OE}"));
        Assert.assertEquals("{\\u230ae}", formatter.format("{\\ae}")); // æ
        Assert.assertEquals("{\\u198AE}", formatter.format("{\\AE}")); // Æ

        Assert.assertEquals("", formatter.format("\\.{o}")); // ???
        Assert.assertEquals("", formatter.format("\\vo")); // ???
        Assert.assertEquals("", formatter.format("\\Ha")); // ã // ???
        Assert.assertEquals("", formatter.format("\\too"));
        Assert.assertEquals("", formatter.format("\\do")); // ???
        Assert.assertEquals("", formatter.format("\\bo")); // ???
        Assert.assertEquals("\\u229a", formatter.format("{\\aa}")); // å
        Assert.assertEquals("\\u197A", formatter.format("{\\AA}")); // Å
        Assert.assertEquals("\\u248o", formatter.format("{\\o}")); // ø
        Assert.assertEquals("\\u216O", formatter.format("{\\O}")); // Ø
        Assert.assertEquals("\\u322l", formatter.format("{\\l}"));
        Assert.assertEquals("\\u321L", formatter.format("{\\L}"));
        Assert.assertEquals("\\u223ss", formatter.format("{\\ss}")); // ß
        Assert.assertEquals("\\u191?", formatter.format("\\`?")); // ¿
        Assert.assertEquals("\\u161!", formatter.format("\\`!")); // ¡

        Assert.assertEquals("", formatter.format("\\dag"));
        Assert.assertEquals("", formatter.format("\\ddag"));
        Assert.assertEquals("\\u167S", formatter.format("{\\S}")); // §
        Assert.assertEquals("\\u182P", formatter.format("{\\P}")); // ¶
        Assert.assertEquals("\\u169?", formatter.format("{\\copyright}")); // ©
        Assert.assertEquals("\\u163?", formatter.format("{\\pounds}")); // £
    }
}
