package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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

        // We should be able to replace the ? with e
        Assert.assertEquals("R\\u233?flexions sur le timing de la quantit\\u233?",
                formatter.format("Réflexions sur le timing de la quantité"));

        Assert.assertEquals("h\\u225allo", formatter.format("h\\'allo"));
        Assert.assertEquals("h\\u225allo", formatter.format("h\\'allo"));
    }

    @Test
    public void testLaTeXHighlighting() {
        Assert.assertEquals("{\\i hallo}", formatter.format("\\emph{hallo}"));
        Assert.assertEquals("{\\i hallo}", formatter.format("{\\emph hallo}"));
        Assert.assertEquals("{An article title with {\\i a book title} emphasized", formatter.format("An article title with \\emph{a book title} emphasized"));

        Assert.assertEquals("{\\i hallo}", formatter.format("\\textit{hallo}"));
        Assert.assertEquals("{\\i hallo}", formatter.format("{\\textit hallo}"));

        Assert.assertEquals("{\\b hallo}", formatter.format("\\textbf{hallo}"));
        Assert.assertEquals("{\\b hallo}", formatter.format("{\\textbf hallo}"));
    }

    @Test
    @Ignore
    public void testComplicated() {
        Assert.assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e \\u230ae should be \\u230ae",
                formatter.format("Réflexions sur le timing de la quantité \\ae should be æ"));

        Assert.assertEquals("h\\u225all{\\uc2\\u339oe}", formatter.format("h\\'all\\oe "));
    }

    @Test
    @Ignore
    public void testSpecialCharacters() {
        Assert.assertEquals("\\u243o", formatter.format("\\'{o}")); // ó
        Assert.assertEquals("\\'f2", formatter.format("\\`{o}")); // ò
        Assert.assertEquals("\\'f4", formatter.format("\\^{o}")); // ô
        Assert.assertEquals("\\'f6", formatter.format("\\\"{o}")); // ö
        Assert.assertEquals("\\u245o", formatter.format("\\~{o}")); // õ
        Assert.assertEquals("\\u333o", formatter.format("\\={o}"));
        Assert.assertEquals("\\u334O", formatter.format("\\u{o}"));
        Assert.assertEquals("\\u231c", formatter.format("\\c{c}")); // ç
        Assert.assertEquals("{\\uc2\\u339oe}", formatter.format("\\oe"));
        Assert.assertEquals("{\\uc2\\u338OE}", formatter.format("\\OE"));
        Assert.assertEquals("{\\uc2\\u230ae}", formatter.format("\\ae")); // æ
        Assert.assertEquals("{\\uc2\\u198AE}", formatter.format("\\AE")); // Æ

        Assert.assertEquals("", formatter.format("\\.{o}")); // ???
        Assert.assertEquals("", formatter.format("\\v{o}")); // ???
        Assert.assertEquals("", formatter.format("\\H{a}")); // ã // ???
        Assert.assertEquals("", formatter.format("\\t{oo}"));
        Assert.assertEquals("", formatter.format("\\d{o}")); // ???
        Assert.assertEquals("", formatter.format("\\b{o}")); // ???
        Assert.assertEquals("", formatter.format("\\aa")); // å
        Assert.assertEquals("", formatter.format("\\AA")); // Å
        Assert.assertEquals("", formatter.format("\\o")); // ø
        Assert.assertEquals("", formatter.format("\\O")); // Ø
        Assert.assertEquals("", formatter.format("\\l"));
        Assert.assertEquals("", formatter.format("\\L"));
        Assert.assertEquals("{\\uc2\\u223ss}", formatter.format("\\ss")); // ß
        Assert.assertEquals("", formatter.format("?`")); // ¿
        Assert.assertEquals("", formatter.format("!`")); // ¡

        Assert.assertEquals("", formatter.format("\\dag"));
        Assert.assertEquals("", formatter.format("\\ddag"));
        Assert.assertEquals("", formatter.format("\\S")); // §
        Assert.assertEquals("", formatter.format("\\P")); // ¶
        Assert.assertEquals("", formatter.format("\\copyright")); // ©
        Assert.assertEquals("", formatter.format("\\pounds")); // £
    }
}
