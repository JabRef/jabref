package net.sf.jabref.logic.util.strings;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.FileField;

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

        assertEquals("", StringUtil.shaveString(null));
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
    }

    @Test
    public void testStripBrackets() {
        assertEquals("foo", StringUtil.stripBrackets("[foo]"));
        assertEquals("[foo]", StringUtil.stripBrackets("[[foo]]"));
        assertEquals("", StringUtil.stripBrackets(""));
        assertEquals("[foo", StringUtil.stripBrackets("[foo"));
        assertEquals("]", StringUtil.stripBrackets("]"));
        assertEquals("", StringUtil.stripBrackets("[]"));
        assertEquals("f[]f", StringUtil.stripBrackets("f[]f"));
        assertEquals(null, StringUtil.stripBrackets(null));
    }

    @Test
    public void testGetPart() {
        // Should be added
    }

    @Test
    public void testFindEncodingsForString() {
        // Unused in JabRef, but should be added in case it finds some use
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
    public void testUnquote() {
        assertEquals("a:", StringUtil.unquote("a::", ':'));
        assertEquals("a:;", StringUtil.unquote("a:::;", ':'));
        assertEquals("a:b%c;", StringUtil.unquote("a::b:%c:;", ':'));
    }


    private static final String[][] stringArray1 = {{"a", "b"}, {"c", "d"}};
    private static final String encStringArray1 = "a:b;c:d";
    private static final String[][] stringArray2null = {{"a", null}, {"c", "d"}};
    private static final String encStringArray2 = "a:;c:d";
    private static final String[][] stringArray2 = {{"a", ""}, {"c", "d"}};
    private static final String encStringArray2null = "a:" + null + ";c:d";
    private static final String[][] stringArray3 = {{"a", ":b"}, {"c;", "d"}};
    private static final String encStringArray3 = "a:\\:b;c\\;:d";


    @Test
    public void testEncodeStringArray() {
        assertEquals(encStringArray1, FileField.encodeStringArray(stringArray1));
        assertEquals(encStringArray2, FileField.encodeStringArray(stringArray2));
        assertEquals(encStringArray2null, FileField.encodeStringArray(stringArray2null));
        assertEquals(encStringArray3, FileField.encodeStringArray(stringArray3));
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

    @Test
    public void testIsInCurlyBrackets() {
        assertFalse(StringUtil.isInCurlyBrackets(""));
        assertFalse(StringUtil.isInCurlyBrackets(null));
        assertTrue(StringUtil.isInCurlyBrackets("{}"));
        assertTrue(StringUtil.isInCurlyBrackets("{a}"));
        assertFalse(StringUtil.isInCurlyBrackets("{"));
        assertFalse(StringUtil.isInCurlyBrackets("}"));
        assertFalse(StringUtil.isInCurlyBrackets("a{}a"));
    }

    @Test
    public void testIsInSquareBrackets() {
        assertFalse(StringUtil.isInSquareBrackets(""));
        assertFalse(StringUtil.isInSquareBrackets(null));
        assertTrue(StringUtil.isInSquareBrackets("[]"));
        assertTrue(StringUtil.isInSquareBrackets("[a]"));
        assertFalse(StringUtil.isInSquareBrackets("["));
        assertFalse(StringUtil.isInSquareBrackets("]"));
        assertFalse(StringUtil.isInSquareBrackets("a[]a"));
    }

    @Test
    public void testIsInCitationMarks() {
        assertFalse(StringUtil.isInCitationMarks(""));
        assertFalse(StringUtil.isInCitationMarks(null));
        assertTrue(StringUtil.isInCitationMarks("\"\""));
        assertTrue(StringUtil.isInCitationMarks("\"a\""));
        assertFalse(StringUtil.isInCitationMarks("\""));
        assertFalse(StringUtil.isInCitationMarks("a\"\"a"));
    }

    @Test
    public void testIntValueOfSingleDigit() {
        assertEquals(1, StringUtil.intValueOf("1"));
        assertEquals(2, StringUtil.intValueOf("2"));
        assertEquals(8, StringUtil.intValueOf("8"));
    }

    @Test
    public void testIntValueOfLongString() {
        assertEquals(1234567890, StringUtil.intValueOf("1234567890"));
    }

    @Test
    public void testIntValueOfStartWithZeros() {
        assertEquals(1234, StringUtil.intValueOf("001234"));
    }

    @Test(expected = NumberFormatException.class)
    public void testIntValueOfExceptionIfStringContainsLetter() {
            StringUtil.intValueOf("12A2");
    }

    @Test(expected = NumberFormatException.class)
    public void testIntValueOfExceptionIfStringNull() {
            StringUtil.intValueOf(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testIntValueOfExceptionfIfStringEmpty() {
            StringUtil.intValueOf("");
    }

    @Test
    public void testQuoteSimple() {
        assertEquals("a::", StringUtil.quote("a:", "", ':'));
    }

    @Test
    public void testQuoteNullQuotation() {
        assertEquals("a::", StringUtil.quote("a:", null, ':'));
    }

    @Test
    public void testQuoteNullString() {
        assertEquals("", StringUtil.quote(null, ";", ':'));
    }

    @Test
    public void testQuoteQuotationCharacter() {
        assertEquals("a:::;", StringUtil.quote("a:;", ";", ':'));
    }

    @Test
    public void testQuoteMoreComplicated() {
        assertEquals("a::b:%c:;", StringUtil.quote("a:b%c;", "%;", ':'));
    }

    @Test
    public void testLimitStringLengthShort() {
        assertEquals("Test", StringUtil.limitStringLength("Test", 20));
    }

    @Test
    public void testLimitStringLengthLimiting() {
        assertEquals("TestTes...", StringUtil.limitStringLength("TestTestTestTestTest", 10));
        assertEquals(10, StringUtil.limitStringLength("TestTestTestTestTest", 10).length());
    }

    @Test
    public void testLimitStringLengthNullInput() {
        assertEquals("", StringUtil.limitStringLength(null, 10));
    }

    @Test
    public void testReplaceSpecialCharacters() {
        Assert.assertEquals("Hallo Arger", StringUtil.replaceSpecialCharacters("Hallo Arger"));
        Assert.assertEquals("aaAeoeeee", StringUtil.replaceSpecialCharacters("åÄöéèë"));
    }

}