package net.sf.jabref.model.util;

import java.util.Optional;

import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.FileField;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModelStringUtilTest {

    @Test
    public void testBooleanToBinaryString() {
        assertEquals("0", ModelStringUtil.booleanToBinaryString(false));
        assertEquals("1", ModelStringUtil.booleanToBinaryString(true));
    }

    @Test
    public void testQuoteSimple() {
        assertEquals("a::", ModelStringUtil.quote("a:", "", ':'));
    }

    @Test
    public void testQuoteNullQuotation() {
        assertEquals("a::", ModelStringUtil.quote("a:", null, ':'));
    }

    @Test
    public void testQuoteNullString() {
        assertEquals("", ModelStringUtil.quote(null, ";", ':'));
    }

    @Test
    public void testQuoteQuotationCharacter() {
        assertEquals("a:::;", ModelStringUtil.quote("a:;", ";", ':'));
    }

    @Test
    public void testQuoteMoreComplicated() {
        assertEquals("a::b:%c:;", ModelStringUtil.quote("a:b%c;", "%;", ':'));
    }

    private static final String[][] STRING_ARRAY_1 = {{"a", "b"}, {"c", "d"}};
    private static final String ENCODED_STRING_ARRAY_1 = "a:b;c:d";
    private static final String[][] STRING_ARRAY_2_WITH_NULL = {{"a", null}, {"c", "d"}};
    private static final String ENCODED_STRING_ARRAY_2_WITH_NULL = "a:" + null + ";c:d";
    private static final String[][] STRING_ARRAY_2 = {{"a", ""}, {"c", "d"}};
    private static final String ENCODED_STRING_ARRAY_2 = "a:;c:d";
    private static final String[][] STRING_ARRAY_3 = {{"a", ":b"}, {"c;", "d"}};
    private static final String ENCODED_STRING_ARRAY_3 = "a:\\:b;c\\;:d";


    @Test
    public void testUnifyLineBreaks() {
        // Mac < v9
        String result = ModelStringUtil.unifyLineBreaksToConfiguredLineBreaks("\r", OS.NEWLINE);
        assertEquals(OS.NEWLINE, result);
        // Windows
        result = ModelStringUtil.unifyLineBreaksToConfiguredLineBreaks("\r\n", OS.NEWLINE);
        assertEquals(OS.NEWLINE, result);
        // Unix
        result = ModelStringUtil.unifyLineBreaksToConfiguredLineBreaks("\n", OS.NEWLINE);
        assertEquals(OS.NEWLINE, result);
    }

    @Test
    public void testGetCorrectFileName() {
        assertEquals("aa.bib", ModelStringUtil.getCorrectFileName("aa", "bib"));
        assertEquals(".login.bib", ModelStringUtil.getCorrectFileName(".login", "bib"));
        assertEquals("a.bib", ModelStringUtil.getCorrectFileName("a.bib", "bib"));
        assertEquals("a.bib", ModelStringUtil.getCorrectFileName("a.bib", "BIB"));
        assertEquals("a.bib", ModelStringUtil.getCorrectFileName("a", "bib"));
        assertEquals("a.bb", ModelStringUtil.getCorrectFileName("a.bb", "bib"));
        assertEquals("", ModelStringUtil.getCorrectFileName(null, "bib"));
    }

    @Test
    public void testQuoteForHTML() {
        assertEquals("&#33;", ModelStringUtil.quoteForHTML("!"));
        assertEquals("&#33;&#33;&#33;", ModelStringUtil.quoteForHTML("!!!"));
    }

    @Test
    public void testRemoveBracesAroundCapitals() {
        assertEquals("ABC", ModelStringUtil.removeBracesAroundCapitals("{ABC}"));
        assertEquals("ABC", ModelStringUtil.removeBracesAroundCapitals("{{ABC}}"));
        assertEquals("{abc}", ModelStringUtil.removeBracesAroundCapitals("{abc}"));
        assertEquals("ABCDEF", ModelStringUtil.removeBracesAroundCapitals("{ABC}{DEF}"));
    }

    @Test
    public void testPutBracesAroundCapitals() {
        assertEquals("{ABC}", ModelStringUtil.putBracesAroundCapitals("ABC"));
        assertEquals("{ABC}", ModelStringUtil.putBracesAroundCapitals("{ABC}"));
        assertEquals("abc", ModelStringUtil.putBracesAroundCapitals("abc"));
        assertEquals("#ABC#", ModelStringUtil.putBracesAroundCapitals("#ABC#"));
        assertEquals("{ABC} def {EFG}", ModelStringUtil.putBracesAroundCapitals("ABC def EFG"));
    }

    @Test
    public void testShaveString() {

        assertEquals("", ModelStringUtil.shaveString(null));
        assertEquals("", ModelStringUtil.shaveString(""));
        assertEquals("aaa", ModelStringUtil.shaveString("   aaa\t\t\n\r"));
        assertEquals("a", ModelStringUtil.shaveString("  {a}    "));
        assertEquals("a", ModelStringUtil.shaveString("  \"a\"    "));
        assertEquals("{a}", ModelStringUtil.shaveString("  {{a}}    "));
        assertEquals("{a}", ModelStringUtil.shaveString("  \"{a}\"    "));
        assertEquals("\"{a\"}", ModelStringUtil.shaveString("  \"{a\"}    "));
    }

    @Test
    public void testJoin() {
        String[] s = "ab/cd/ed".split("/");
        assertEquals("ab\\cd\\ed", ModelStringUtil.join(s, "\\", 0, s.length));

        assertEquals("cd\\ed", ModelStringUtil.join(s, "\\", 1, s.length));

        assertEquals("ed", ModelStringUtil.join(s, "\\", 2, s.length));

        assertEquals("", ModelStringUtil.join(s, "\\", 3, s.length));

        assertEquals("", ModelStringUtil.join(new String[] {}, "\\", 0, 0));
    }

    @Test
    public void testStripBrackets() {
        assertEquals("foo", ModelStringUtil.stripBrackets("[foo]"));
        assertEquals("[foo]", ModelStringUtil.stripBrackets("[[foo]]"));
        assertEquals("", ModelStringUtil.stripBrackets(""));
        assertEquals("[foo", ModelStringUtil.stripBrackets("[foo"));
        assertEquals("]", ModelStringUtil.stripBrackets("]"));
        assertEquals("", ModelStringUtil.stripBrackets("[]"));
        assertEquals("f[]f", ModelStringUtil.stripBrackets("f[]f"));
        assertEquals(null, ModelStringUtil.stripBrackets(null));
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
        assertEquals("aaaaa" + OS.NEWLINE + "\tbbbbb" + OS.NEWLINE + "\tccccc",
                ModelStringUtil.wrap("aaaaa bbbbb ccccc", 5, OS.NEWLINE));
        assertEquals("aaaaa bbbbb" + OS.NEWLINE + "\tccccc", ModelStringUtil.wrap("aaaaa bbbbb ccccc", 8, OS.NEWLINE));
        assertEquals("aaaaa bbbbb" + OS.NEWLINE + "\tccccc", ModelStringUtil.wrap("aaaaa bbbbb ccccc", 11, OS.NEWLINE));
        assertEquals("aaaaa bbbbb ccccc", ModelStringUtil.wrap("aaaaa bbbbb ccccc", 12, OS.NEWLINE));
        assertEquals("aaaaa" + OS.NEWLINE + "\t" + OS.NEWLINE + "\tbbbbb" + OS.NEWLINE + "\t"
                + OS.NEWLINE + "\tccccc", ModelStringUtil.wrap("aaaaa\nbbbbb\nccccc", 12, OS.NEWLINE));
        assertEquals(
                "aaaaa" + OS.NEWLINE + "\t" + OS.NEWLINE + "\t" + OS.NEWLINE + "\tbbbbb"
                        + OS.NEWLINE + "\t" + OS.NEWLINE + "\tccccc",
                ModelStringUtil.wrap("aaaaa\n\nbbbbb\nccccc", 12, OS.NEWLINE));
        assertEquals("aaaaa" + OS.NEWLINE + "\t" + OS.NEWLINE + "\tbbbbb" + OS.NEWLINE + "\t"
                + OS.NEWLINE + "\tccccc", ModelStringUtil.wrap("aaaaa\r\nbbbbb\r\nccccc", 12, OS.NEWLINE));
    }

    @Test
    public void testEncodeStringArray() {
        assertEquals(ENCODED_STRING_ARRAY_1, FileField.encodeStringArray(STRING_ARRAY_1));
        assertEquals(ENCODED_STRING_ARRAY_2, FileField.encodeStringArray(STRING_ARRAY_2));
        assertEquals(ENCODED_STRING_ARRAY_2_WITH_NULL, FileField.encodeStringArray(STRING_ARRAY_2_WITH_NULL));
        assertEquals(ENCODED_STRING_ARRAY_3, FileField.encodeStringArray(STRING_ARRAY_3));
    }

    @Test
    public void testDecodeStringDoubleArray() {
        assertArrayEquals(STRING_ARRAY_1, ModelStringUtil.decodeStringDoubleArray(ENCODED_STRING_ARRAY_1));
        assertArrayEquals(STRING_ARRAY_2, ModelStringUtil.decodeStringDoubleArray(ENCODED_STRING_ARRAY_2));
        // arrays first differed at element [0][1]; expected: null<null> but was: java.lang.String<null>
        // assertArrayEquals(stringArray2res, ModelStringUtil.decodeStringDoubleArray(encStringArray2));
        assertArrayEquals(STRING_ARRAY_3, ModelStringUtil.decodeStringDoubleArray(ENCODED_STRING_ARRAY_3));
    }

    @Test
    public void testIsInCurlyBrackets() {
        assertFalse(ModelStringUtil.isInCurlyBrackets(""));
        assertFalse(ModelStringUtil.isInCurlyBrackets(null));
        assertTrue(ModelStringUtil.isInCurlyBrackets("{}"));
        assertTrue(ModelStringUtil.isInCurlyBrackets("{a}"));
        assertTrue(ModelStringUtil.isInCurlyBrackets("{a{a}}"));
        assertTrue(ModelStringUtil.isInCurlyBrackets("{{\\AA}sa {\\AA}Stor{\\aa}}"));
        assertFalse(ModelStringUtil.isInCurlyBrackets("{"));
        assertFalse(ModelStringUtil.isInCurlyBrackets("}"));
        assertFalse(ModelStringUtil.isInCurlyBrackets("a{}a"));
        assertFalse(ModelStringUtil.isInCurlyBrackets("{\\AA}sa {\\AA}Stor{\\aa}"));

    }

    @Test
    public void testIsInSquareBrackets() {
        assertFalse(ModelStringUtil.isInSquareBrackets(""));
        assertFalse(ModelStringUtil.isInSquareBrackets(null));
        assertTrue(ModelStringUtil.isInSquareBrackets("[]"));
        assertTrue(ModelStringUtil.isInSquareBrackets("[a]"));
        assertFalse(ModelStringUtil.isInSquareBrackets("["));
        assertFalse(ModelStringUtil.isInSquareBrackets("]"));
        assertFalse(ModelStringUtil.isInSquareBrackets("a[]a"));
    }

    @Test
    public void testIsInCitationMarks() {
        assertFalse(ModelStringUtil.isInCitationMarks(""));
        assertFalse(ModelStringUtil.isInCitationMarks(null));
        assertTrue(ModelStringUtil.isInCitationMarks("\"\""));
        assertTrue(ModelStringUtil.isInCitationMarks("\"a\""));
        assertFalse(ModelStringUtil.isInCitationMarks("\""));
        assertFalse(ModelStringUtil.isInCitationMarks("a\"\"a"));
    }

    @Test
    public void testIntValueOfSingleDigit() {
        assertEquals(1, ModelStringUtil.intValueOf("1"));
        assertEquals(2, ModelStringUtil.intValueOf("2"));
        assertEquals(8, ModelStringUtil.intValueOf("8"));
    }

    @Test
    public void testIntValueOfLongString() {
        assertEquals(1234567890, ModelStringUtil.intValueOf("1234567890"));
    }

    @Test
    public void testIntValueOfStartWithZeros() {
        assertEquals(1234, ModelStringUtil.intValueOf("001234"));
    }

    @Test(expected = NumberFormatException.class)
    public void testIntValueOfExceptionIfStringContainsLetter() {
        ModelStringUtil.intValueOf("12A2");
    }

    @Test(expected = NumberFormatException.class)
    public void testIntValueOfExceptionIfStringNull() {
        ModelStringUtil.intValueOf(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testIntValueOfExceptionfIfStringEmpty() {
        ModelStringUtil.intValueOf("");
    }

    @Test
    public void testIntValueOfWithNullSingleDigit() {
        assertEquals(Optional.of(Integer.valueOf(1)), ModelStringUtil.intValueOfOptional("1"));
        assertEquals(Optional.of(Integer.valueOf(2)), ModelStringUtil.intValueOfOptional("2"));
        assertEquals(Optional.of(Integer.valueOf(8)), ModelStringUtil.intValueOfOptional("8"));
    }

    @Test
    public void testIntValueOfWithNullLongString() {
        assertEquals(Optional.of(Integer.valueOf(1234567890)), ModelStringUtil.intValueOfOptional("1234567890"));
    }

    @Test
    public void testIntValueOfWithNullStartWithZeros() {
        assertEquals(Optional.of(Integer.valueOf(1234)), ModelStringUtil.intValueOfOptional("001234"));
    }

    @Test
    public void testIntValueOfWithNullExceptionIfStringContainsLetter() {
        assertEquals(Optional.empty(), ModelStringUtil.intValueOfOptional("12A2"));
    }

    @Test
    public void testIntValueOfWithNullExceptionIfStringNull() {
        assertEquals(Optional.empty(), ModelStringUtil.intValueOfOptional(null));
    }

    @Test
    public void testIntValueOfWithNullExceptionfIfStringEmpty() {
        assertEquals(Optional.empty(), ModelStringUtil.intValueOfOptional(""));
    }

    @Test
    public void testLimitStringLengthShort() {
        assertEquals("Test", ModelStringUtil.limitStringLength("Test", 20));
    }

    @Test
    public void testLimitStringLengthLimiting() {
        assertEquals("TestTes...", ModelStringUtil.limitStringLength("TestTestTestTestTest", 10));
        assertEquals(10, ModelStringUtil.limitStringLength("TestTestTestTestTest", 10).length());
    }

    @Test
    public void testLimitStringLengthNullInput() {
        assertEquals("", ModelStringUtil.limitStringLength(null, 10));
    }

    @Test
    public void testReplaceSpecialCharacters() {
        assertEquals("Hallo Arger", ModelStringUtil.replaceSpecialCharacters("Hallo Arger"));
        assertEquals("aaAeoeeee", ModelStringUtil.replaceSpecialCharacters("åÄöéèë"));
    }

    @Test
    public void testRepeatSpaces() {
        assertEquals("", ModelStringUtil.repeatSpaces(0));
        assertEquals(" ", ModelStringUtil.repeatSpaces(1));
        assertEquals("       ", ModelStringUtil.repeatSpaces(7));
    }

    @Test
    public void testRepeat() {
        assertEquals("", ModelStringUtil.repeat(0, 'a'));
        assertEquals("a", ModelStringUtil.repeat(1, 'a'));
        assertEquals("aaaaaaa", ModelStringUtil.repeat(7, 'a'));
    }

    @Test
    public void testBoldHTML() {
        assertEquals("<b>AA</b>", ModelStringUtil.boldHTML("AA"));
    }

    @Test
    public void testBoldHTMLReturnsOriginalTextIfNonNull() {
        assertEquals("<b>AA</b>", ModelStringUtil.boldHTML("AA", "BB"));
    }

    @Test
    public void testBoldHTMLReturnsAlternativeTextIfNull() {
        assertEquals("<b>BB</b>", ModelStringUtil.boldHTML(null, "BB"));
    }

    @Test
    public void testUnquote() {
        assertEquals("a:", ModelStringUtil.unquote("a::", ':'));
        assertEquals("a:;", ModelStringUtil.unquote("a:::;", ':'));
        assertEquals("a:b%c;", ModelStringUtil.unquote("a::b:%c:;", ':'));
    }
}
