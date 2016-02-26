package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class RTFCharsTest {

    @Test
    public void testBasicFormat() {

        LayoutFormatter layout = new RTFChars();

        Assert.assertEquals("", layout.format(""));

        Assert.assertEquals("hallo", layout.format("hallo"));

        // We should be able to replace the ? with e
        Assert.assertEquals("R\\u233?flexions sur le timing de la quantit\\u233?",
                layout.format("Réflexions sur le timing de la quantité"));

        Assert.assertEquals("h\\u225allo", layout.format("h\\'allo"));
        Assert.assertEquals("h\\u225allo", layout.format("h\\'allo"));
    }

    @Test
    public void testLaTeXHighlighting() {

        LayoutFormatter layout = new RTFChars();

        Assert.assertEquals("{\\i hallo}", layout.format("\\emph{hallo}"));
        Assert.assertEquals("{\\i hallo}", layout.format("{\\emph hallo}"));

        Assert.assertEquals("{\\i hallo}", layout.format("\\textit{hallo}"));
        Assert.assertEquals("{\\i hallo}", layout.format("{\\textit hallo}"));

        Assert.assertEquals("{\\b hallo}", layout.format("\\textbf{hallo}"));
        Assert.assertEquals("{\\b hallo}", layout.format("{\\textbf hallo}"));
    }

    @Test
    @Ignore
    public void testComplicated() {
        LayoutFormatter layout = new RTFChars();

        Assert.assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e \\u230ae should be \\u230ae",
                layout.format("Réflexions sur le timing de la quantité \\ae should be æ"));

        Assert.assertEquals("h\\u225all{\\uc2\\u339oe}", layout.format("h\\'all\\oe "));
    }

    @Test
    @Ignore
    public void testSpecialCharacters() {

        LayoutFormatter layout = new RTFChars();

        Assert.assertEquals("\\u243o", layout.format("\\'{o}")); // ó
        Assert.assertEquals("\\'f2", layout.format("\\`{o}")); // ò
        Assert.assertEquals("\\'f4", layout.format("\\^{o}")); // ô
        Assert.assertEquals("\\'f6", layout.format("\\\"{o}")); // ö
        Assert.assertEquals("\\u245o", layout.format("\\~{o}")); // õ
        Assert.assertEquals("\\u333o", layout.format("\\={o}"));
        Assert.assertEquals("\\u334O", layout.format("\\u{o}"));
        Assert.assertEquals("\\u231c", layout.format("\\c{c}")); // ç
        Assert.assertEquals("{\\uc2\\u339oe}", layout.format("\\oe"));
        Assert.assertEquals("{\\uc2\\u338OE}", layout.format("\\OE"));
        Assert.assertEquals("{\\uc2\\u230ae}", layout.format("\\ae")); // æ
        Assert.assertEquals("{\\uc2\\u198AE}", layout.format("\\AE")); // Æ

        Assert.assertEquals("", layout.format("\\.{o}")); // ???
        Assert.assertEquals("", layout.format("\\v{o}")); // ???
        Assert.assertEquals("", layout.format("\\H{a}")); // ã // ???
        Assert.assertEquals("", layout.format("\\t{oo}"));
        Assert.assertEquals("", layout.format("\\d{o}")); // ???
        Assert.assertEquals("", layout.format("\\b{o}")); // ???
        Assert.assertEquals("", layout.format("\\aa")); // å
        Assert.assertEquals("", layout.format("\\AA")); // Å
        Assert.assertEquals("", layout.format("\\o")); // ø
        Assert.assertEquals("", layout.format("\\O")); // Ø
        Assert.assertEquals("", layout.format("\\l"));
        Assert.assertEquals("", layout.format("\\L"));
        Assert.assertEquals("{\\uc2\\u223ss}", layout.format("\\ss")); // ß
        Assert.assertEquals("", layout.format("?`")); // ¿
        Assert.assertEquals("", layout.format("!`")); // ¡

        Assert.assertEquals("", layout.format("\\dag"));
        Assert.assertEquals("", layout.format("\\ddag"));
        Assert.assertEquals("", layout.format("\\S")); // §
        Assert.assertEquals("", layout.format("\\P")); // ¶
        Assert.assertEquals("", layout.format("\\copyright")); // ©
        Assert.assertEquals("", layout.format("\\pounds")); // £
    }
}
