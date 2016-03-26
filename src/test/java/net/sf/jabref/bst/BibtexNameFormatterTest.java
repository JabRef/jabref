package net.sf.jabref.bst;

import net.sf.jabref.model.entry.AuthorList;

import org.junit.Assert;
import org.junit.Test;

public class BibtexNameFormatterTest {

    @Test
    public void testFormatName() {
        {
            AuthorList al = AuthorList
                    .parse("Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");

            Assert.assertEquals("de~laVall{\\'e}e~PoussinCharles Louis Xavier~Joseph", BibtexNameFormatter.formatName(al
                    .getAuthor(0), "{vv}{ll}{jj}{ff}", Assert::fail));
        }

        {
            AuthorList al = AuthorList
                    .parse("Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");

            Assert.assertEquals("de~la Vall{\\'e}e~Poussin, C.~L. X.~J.", BibtexNameFormatter.formatName(al
                    .getAuthor(0), "{vv~}{ll}{, jj}{, f.}", Assert::fail));
        }

        {
            AuthorList al = AuthorList
                    .parse("Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");

            Assert.assertEquals("de~la Vall{\\'e}e~Poussin, C.~L. X.~J?", BibtexNameFormatter.formatName(al
                    .getAuthor(0), "{vv~}{ll}{, jj}{, f}?", Assert::fail));
        }

        AuthorList al = AuthorList
                .parse("Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");

        Assert.assertEquals("dlVP", BibtexNameFormatter.formatName(al.getAuthor(0), "{v{}}{l{}}",
                Assert::fail
                ));

        assertNameFormatA("Meyer, J?", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
        assertNameFormatB("J.~Meyer", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
        assertNameFormatC("Jonathan Meyer", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
        assertNameFormatA("Masterly, {\\'{E}}?", "{\\'{E}}douard Masterly");
        assertNameFormatB("{\\'{E}}.~Masterly", "{\\'{E}}douard Masterly");
        assertNameFormatC("{\\'{E}}douard Masterly", "{\\'{E}}douard Masterly");
        assertNameFormatA("{\\\"{U}}nderwood, U?", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
        assertNameFormatB("U.~{\\\"{U}}nderwood", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
        assertNameFormatC("Ulrich {\\\"{U}}nderwood", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
        assertNameFormatA("Victor, P.~{\\'E}?", "Paul {\\'E}mile Victor and and de la Cierva y Codorn{\\’\\i}u, Juan");
        assertNameFormatB("P.~{\\'E}. Victor", "Paul {\\'E}mile Victor and and de la Cierva y Codorn{\\’\\i}u, Juan");
        assertNameFormatC("Paul~{\\'E}mile Victor",
                "Paul {\\'E}mile Victor and and de la Cierva y Codorn{\\’\\i}u, Juan");

    }

    private void assertNameFormat(String string, String string2, int which, String format) {
        Assert.assertEquals(string, BibtexNameFormatter.formatName(string2, which, format,
                Assert::fail
                ));
    }

    private void assertNameFormatC(String string, String string2) {
        assertNameFormat(string, string2, 1, "{ff }{vv }{ll}{ jj}");
    }

    private void assertNameFormatB(String string, String string2) {
        assertNameFormat(string, string2, 1, "{f.~}{vv~}{ll}{, jj}");
    }

    private void assertNameFormatA(String string, String string2) {
        assertNameFormat(string, string2, 1, "{vv~}{ll}{, jj}{, f}?");
    }

    @Test
    public void testConsumeToMatchingBrace() {
        {
            StringBuilder sb = new StringBuilder();
            Assert.assertEquals(6, BibtexNameFormatter.consumeToMatchingBrace(sb, "{HELLO} {WORLD}"
                    .toCharArray(), 0));
            Assert.assertEquals("{HELLO}", sb.toString());
        }
        {
            StringBuilder sb = new StringBuilder();
            Assert.assertEquals(18, BibtexNameFormatter.consumeToMatchingBrace(sb, "{HE{L{}L}O} {WORLD}"
                    .toCharArray(), 12));
            Assert.assertEquals("{WORLD}", sb.toString());
        }
        StringBuilder sb = new StringBuilder();
        Assert.assertEquals(10, BibtexNameFormatter.consumeToMatchingBrace(sb, "{HE{L{}L}O} {WORLD}"
                .toCharArray(), 0));
        Assert.assertEquals("{HE{L{}L}O}", sb.toString());
    }

    @Test
    public void testGetFirstCharOfString() {
        Assert.assertEquals("C", BibtexNameFormatter.getFirstCharOfString("Charles"));
        Assert.assertEquals("V", BibtexNameFormatter.getFirstCharOfString("Vall{\\'e}e"));
        Assert.assertEquals("{\\'e}", BibtexNameFormatter.getFirstCharOfString("{\\'e}"));
        Assert.assertEquals("{\\'e", BibtexNameFormatter.getFirstCharOfString("{\\'e"));
        Assert.assertEquals("E", BibtexNameFormatter.getFirstCharOfString("{E"));
    }

    @Test
    public void testNumberOfChars() {

        Assert.assertEquals(6, BibtexNameFormatter.numberOfChars("Vall{\\'e}e", -1));
        Assert.assertEquals(2, BibtexNameFormatter.numberOfChars("Vall{\\'e}e", 2));
        Assert.assertEquals(1, BibtexNameFormatter.numberOfChars("Vall{\\'e}e", 1));
        Assert.assertEquals(6, BibtexNameFormatter.numberOfChars("Vall{\\'e}e", 6));
        Assert.assertEquals(6, BibtexNameFormatter.numberOfChars("Vall{\\'e}e", 7));
        Assert.assertEquals(8, BibtexNameFormatter.numberOfChars("Vall{e}e", -1));
        Assert.assertEquals(6, BibtexNameFormatter.numberOfChars("Vall{\\'e this will be skipped}e", -1));
    }

}
