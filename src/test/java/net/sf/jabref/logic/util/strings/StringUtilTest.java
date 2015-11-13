package net.sf.jabref.logic.util.strings;

import static org.junit.Assert.*;

import org.junit.Assert;
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
        Assert.assertEquals("aa.bib", StringUtil.getCorrectFileName("aa", "bib"));
        Assert.assertEquals(".login.bib", StringUtil.getCorrectFileName(".login", "bib"));
        Assert.assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "bib"));
        Assert.assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "BIB"));
        Assert.assertEquals("a.bib", StringUtil.getCorrectFileName("a", "bib"));
        Assert.assertEquals("a.bb", StringUtil.getCorrectFileName("a.bb", "bib"));
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
        assertEquals("{ABC} def {EFG}", StringUtil.putBracesAroundCapitals("ABC def EFG"));
    }

    @Test
    public void testShaveString() {

        Assert.assertEquals(null, StringUtil.shaveString(null));
        Assert.assertEquals("", StringUtil.shaveString(""));
        Assert.assertEquals("aaa", StringUtil.shaveString("   aaa\t\t\n\r"));
        Assert.assertEquals("a", StringUtil.shaveString("  {a}    "));
        Assert.assertEquals("a", StringUtil.shaveString("  \"a\"    "));
        Assert.assertEquals("{a}", StringUtil.shaveString("  {{a}}    "));
        Assert.assertEquals("{a}", StringUtil.shaveString("  \"{a}\"    "));
        Assert.assertEquals("\"{a\"}", StringUtil.shaveString("  \"{a\"}    "));
    }

    @Test
    public void testJoin() {
        String[] s = "ab/cd/ed".split("/");
        Assert.assertEquals("ab\\cd\\ed", StringUtil.join(s, "\\", 0, s.length));

        Assert.assertEquals("cd\\ed", StringUtil.join(s, "\\", 1, s.length));

        Assert.assertEquals("ed", StringUtil.join(s, "\\", 2, s.length));

        Assert.assertEquals("", StringUtil.join(s, "\\", 3, s.length));

        Assert.assertEquals("", StringUtil.join(new String[] {}, "\\", 0, 0));
    }

    @Test
    public void testStripBrackets() {
        Assert.assertEquals("foo", StringUtil.stripBrackets("[foo]"));
        Assert.assertEquals("[foo]", StringUtil.stripBrackets("[[foo]]"));
        Assert.assertEquals("foo", StringUtil.stripBrackets("foo]"));
        Assert.assertEquals("foo", StringUtil.stripBrackets("[foo"));
        Assert.assertEquals("", StringUtil.stripBrackets(""));
        Assert.assertEquals("", StringUtil.stripBrackets("[]"));
        Assert.assertEquals("", StringUtil.stripBrackets("["));
        Assert.assertEquals("", StringUtil.stripBrackets("]"));
        Assert.assertEquals("f[]f", StringUtil.stripBrackets("f[]f"));

        try {
            StringUtil.stripBrackets(null);
            Assert.fail();
        } catch (NullPointerException ignored) {
            // Ignored
        }
    }
}