package net.sf.jabref.logic.util.strings;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

public class StringUtilTest {
    @BeforeClass
    public static void loadPreferences() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testUnifyLineBreaks() throws Exception {
        // Mac < v9
        String result = StringUtil.unifyLineBreaksToConfiguredLineBreaks("\r");
        assertEquals(Globals.NEWLINE, result);
        // Windows
        result = StringUtil.unifyLineBreaksToConfiguredLineBreaks("\r\n");
        assertEquals(Globals.NEWLINE, result);
        // Unix
        result = StringUtil.unifyLineBreaksToConfiguredLineBreaks("\n");
        assertEquals(Globals.NEWLINE, result);
    }

    @Test
    public void testGetCorrectFileName() {
        assertEquals("aa.bib", StringUtil.getCorrectFileName("aa", "bib"));
        assertEquals(".login.bib", StringUtil.getCorrectFileName(".login", "bib"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "bib"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "BIB"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a", "bib"));
        assertEquals("a.bb", StringUtil.getCorrectFileName("a.bb", "bib"));
        assertEquals("", StringUtil.getCorrectFileName(null, "bib"));
    }

    @Test
    public void testQuoteForHTML() {
        assertEquals("&#33;", StringUtil.quoteForHTML("!"));
        assertEquals("&#33;&#33;&#33;", StringUtil.quoteForHTML("!!!"));
    }

    @Test
    public void testRemoveBracesAroundCapitals() {
        assertEquals("ABC", StringUtil.removeBracesAroundCapitals("{ABC}"));
        assertEquals("ABC", StringUtil.removeBracesAroundCapitals("{{ABC}}"));
        assertEquals("{abc}", StringUtil.removeBracesAroundCapitals("{abc}"));
        assertEquals("ABCDEF", StringUtil.removeBracesAroundCapitals("{ABC}{DEF}"));
    }

    @Test
    public void testPutBracesAroundCapitals() {
        assertEquals("{ABC}", StringUtil.putBracesAroundCapitals("ABC"));
        assertEquals("{ABC}", StringUtil.putBracesAroundCapitals("{ABC}"));
        assertEquals("abc", StringUtil.putBracesAroundCapitals("abc"));
        assertEquals("#ABC#", StringUtil.putBracesAroundCapitals("#ABC#"));
        assertEquals("{ABC} def {EFG}", StringUtil.putBracesAroundCapitals("ABC def EFG"));
    }

    @Test
    public void testShaveString() {

        assertEquals(null, StringUtil.shaveString(null));
        assertEquals("", StringUtil.shaveString(""));
        assertEquals("aaa", StringUtil.shaveString("   aaa\t\t\n\r"));
        assertEquals("a", StringUtil.shaveString("  {a}    "));
        assertEquals("a", StringUtil.shaveString("  \"a\"    "));
        assertEquals("{a}", StringUtil.shaveString("  {{a}}    "));
        assertEquals("{a}", StringUtil.shaveString("  \"{a}\"    "));
        assertEquals("\"{a\"}", StringUtil.shaveString("  \"{a\"}    "));
    }

    @Test
    public void testJoin() {
        String[] s = "ab/cd/ed".split("/");
        assertEquals("ab\\cd\\ed", StringUtil.join(s, "\\", 0, s.length));

        assertEquals("cd\\ed", StringUtil.join(s, "\\", 1, s.length));

        assertEquals("ed", StringUtil.join(s, "\\", 2, s.length));

        assertEquals("", StringUtil.join(s, "\\", 3, s.length));

        assertEquals("", StringUtil.join(new String[] {}, "\\", 0, 0));

        assertEquals("a:b", StringUtil.join(stringArray1[0], ":"));
    }

    @Test
    public void testStripBrackets() {
        assertEquals("foo", StringUtil.stripBrackets("[foo]"));
        assertEquals("[foo]", StringUtil.stripBrackets("[[foo]]"));
        assertEquals("foo", StringUtil.stripBrackets("foo]"));
        assertEquals("foo", StringUtil.stripBrackets("[foo"));
        assertEquals("", StringUtil.stripBrackets(""));
        assertEquals("", StringUtil.stripBrackets("[]"));
        assertEquals("", StringUtil.stripBrackets("["));
        assertEquals("", StringUtil.stripBrackets("]"));
        assertEquals("f[]f", StringUtil.stripBrackets("f[]f"));

        try {
            StringUtil.stripBrackets(null);
            fail();
        } catch (NullPointerException ignored) {
            // Ignored
        }
    }

    @Test
    public void testGetPart() {

    }

    @Test
    public void testWrap() {
        assertEquals("aaaaa" + Globals.NEWLINE + "\tbbbbb" + Globals.NEWLINE + "\tccccc",
                StringUtil.wrap("aaaaa bbbbb ccccc", 5));
        assertEquals("aaaaa bbbbb" + Globals.NEWLINE + "\tccccc", StringUtil.wrap("aaaaa bbbbb ccccc", 8));
        assertEquals("aaaaa bbbbb" + Globals.NEWLINE + "\tccccc", StringUtil.wrap("aaaaa bbbbb ccccc", 11));
        assertEquals("aaaaa bbbbb ccccc", StringUtil.wrap("aaaaa bbbbb ccccc", 12));
        assertEquals("aaaaa" + Globals.NEWLINE + "\t" + Globals.NEWLINE + "\tbbbbb" + Globals.NEWLINE + "\t"
                + Globals.NEWLINE + "\tccccc", StringUtil.wrap("aaaaa\nbbbbb\nccccc", 12));
        assertEquals(
                "aaaaa" + Globals.NEWLINE + "\t" + Globals.NEWLINE + "\t" + Globals.NEWLINE + "\tbbbbb"
                        + Globals.NEWLINE + "\t" + Globals.NEWLINE + "\tccccc",
                StringUtil.wrap("aaaaa\n\nbbbbb\nccccc", 12));
        assertEquals("aaaaa" + Globals.NEWLINE + "\t" + Globals.NEWLINE + "\tbbbbb" + Globals.NEWLINE + "\t"
                + Globals.NEWLINE + "\tccccc", StringUtil.wrap("aaaaa\r\nbbbbb\r\nccccc", 12));
    }

    @Test
    public void testQuote() {
        assertEquals("a::", StringUtil.quote("a:", "", ':'));
        assertEquals("a::", StringUtil.quote("a:", null, ':'));
        assertEquals("a:::;", StringUtil.quote("a:;", ";", ':'));
        assertEquals("a::b:%c:;", StringUtil.quote("a:b%c;", "%;", ':'));
    }

    @Test
    public void testUnquote() {
        assertEquals("a:", StringUtil.unquote("a::", ':'));
        assertEquals("a:;", StringUtil.unquote("a:::;", ':'));
        assertEquals("a:b%c;", StringUtil.unquote("a::b:%c:;", ':'));
    }


    String[][] stringArray1 = {{"a", "b"}, {"c", "d"}};
    String encStringArray1 = "a:b;c:d";
    String[][] stringArray2null = {{"a", null}, {"c", "d"}};
    String encStringArray2 = "a:;c:d";
    String[][] stringArray2 = {{"a", ""}, {"c", "d"}};
    String encStringArray2null = "a:" + null + ";c:d";
    String[][] stringArray3 = {{"a", ":b"}, {"c;", "d"}};
    String encStringArray3 = "a:\\:b;c\\;:d";


    @Test
    public void testEncodeStringArray() {
        assertEquals(encStringArray1, StringUtil.encodeStringArray(stringArray1));
        assertEquals(encStringArray2, StringUtil.encodeStringArray(stringArray2));
        assertEquals(encStringArray2null, StringUtil.encodeStringArray(stringArray2null));
        assertEquals(encStringArray3, StringUtil.encodeStringArray(stringArray3));
    }

    @Test
    public void testDecodeStringDoubleArray() {
        assertArrayEquals(stringArray1, StringUtil.decodeStringDoubleArray(encStringArray1));
        assertArrayEquals(stringArray2, StringUtil.decodeStringDoubleArray(encStringArray2));
        // arrays first differed at element [0][1]; expected: null<null> but was: java.lang.String<null>
        // assertArrayEquals(stringArray2res, StringUtil.decodeStringDoubleArray(encStringArray2));
        assertArrayEquals(stringArray3, StringUtil.decodeStringDoubleArray(encStringArray3));
    }

    @Test
    public void testBooleanToBinaryString() {
        assertEquals("0", StringUtil.booleanToBinaryString(false));
        assertEquals("1", StringUtil.booleanToBinaryString(true));
    }

}