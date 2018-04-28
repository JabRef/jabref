package org.jabref.model.strings;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringUtilTest {

    @Test
    public void StringUtilClassIsSmall() throws Exception {
        Path path = Paths.get("src", "main", "java", StringUtil.class.getName().replace('.', '/') + ".java");
        int lineCount = Files.readAllLines(path, StandardCharsets.UTF_8).size();

        assertTrue(lineCount <= 722, "StringUtil increased in size. "
                + "We try to keep this class as small as possible. "
                + "Thus think twice if you add something to StringUtil.");
    }

    @Test
    public void testBooleanToBinaryString() {
        assertEquals("0", StringUtil.booleanToBinaryString(false));
        assertEquals("1", StringUtil.booleanToBinaryString(true));
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
    public void testUnifyLineBreaks() {
        // Mac < v9
        String result = StringUtil.unifyLineBreaks("\r", "newline");
        assertEquals("newline", result);
        // Windows
        result = StringUtil.unifyLineBreaks("\r\n", "newline");
        assertEquals("newline", result);
        // Unix
        result = StringUtil.unifyLineBreaks("\n", "newline");
        assertEquals("newline", result);
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
        String newline = "newline";
        assertEquals("aaaaa" + newline + "\tbbbbb" + newline + "\tccccc",
                StringUtil.wrap("aaaaa bbbbb ccccc", 5, newline));
        assertEquals("aaaaa bbbbb" + newline + "\tccccc", StringUtil.wrap("aaaaa bbbbb ccccc", 8, newline));
        assertEquals("aaaaa bbbbb" + newline + "\tccccc", StringUtil.wrap("aaaaa bbbbb ccccc", 11, newline));
        assertEquals("aaaaa bbbbb ccccc", StringUtil.wrap("aaaaa bbbbb ccccc", 12, newline));
        assertEquals("aaaaa" + newline + "\t" + newline + "\tbbbbb" + newline + "\t" + newline + "\tccccc",
                StringUtil.wrap("aaaaa\nbbbbb\nccccc", 12, newline));
        assertEquals(
                "aaaaa" + newline + "\t" + newline + "\t" + newline + "\tbbbbb" + newline + "\t" + newline + "\tccccc",
                StringUtil.wrap("aaaaa\n\nbbbbb\nccccc", 12, newline));
        assertEquals("aaaaa" + newline + "\t" + newline + "\tbbbbb" + newline + "\t" + newline + "\tccccc",
                StringUtil.wrap("aaaaa\r\nbbbbb\r\nccccc", 12, newline));
    }

    @Test
    public void testDecodeStringDoubleArray() {
        assertArrayEquals(new String[][]{{"a", "b"}, {"c", "d"}}, StringUtil.decodeStringDoubleArray("a:b;c:d"));
        assertArrayEquals(new String[][]{{"a", ""}, {"c", "d"}}, StringUtil.decodeStringDoubleArray("a:;c:d"));
        // arrays first differed at element [0][1]; expected: null<null> but was: java.lang.String<null>
        // assertArrayEquals(stringArray2res, StringUtil.decodeStringDoubleArray(encStringArray2));
        assertArrayEquals(new String[][]{{"a", ":b"}, {"c;", "d"}}, StringUtil.decodeStringDoubleArray("a:\\:b;c\\;:d"));
    }

    @Test
    public void testIsInCurlyBrackets() {
        assertFalse(StringUtil.isInCurlyBrackets(""));
        assertFalse(StringUtil.isInCurlyBrackets(null));
        assertTrue(StringUtil.isInCurlyBrackets("{}"));
        assertTrue(StringUtil.isInCurlyBrackets("{a}"));
        assertTrue(StringUtil.isInCurlyBrackets("{a{a}}"));
        assertTrue(StringUtil.isInCurlyBrackets("{{\\AA}sa {\\AA}Stor{\\aa}}"));
        assertFalse(StringUtil.isInCurlyBrackets("{"));
        assertFalse(StringUtil.isInCurlyBrackets("}"));
        assertFalse(StringUtil.isInCurlyBrackets("a{}a"));
        assertFalse(StringUtil.isInCurlyBrackets("{\\AA}sa {\\AA}Stor{\\aa}"));

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

    @Test
    public void testIntValueOfExceptionIfStringContainsLetter() {
        assertThrows(NumberFormatException.class, () -> StringUtil.intValueOf("12A2"));
    }

    @Test
    public void testIntValueOfExceptionIfStringNull() {
        assertThrows(NumberFormatException.class, () -> StringUtil.intValueOf(null));
    }

    @Test
    public void testIntValueOfExceptionfIfStringEmpty() {
        assertThrows(NumberFormatException.class, () -> StringUtil.intValueOf(""));
    }

    @Test
    public void testIntValueOfWithNullSingleDigit() {
        assertEquals(Optional.of(Integer.valueOf(1)), StringUtil.intValueOfOptional("1"));
        assertEquals(Optional.of(Integer.valueOf(2)), StringUtil.intValueOfOptional("2"));
        assertEquals(Optional.of(Integer.valueOf(8)), StringUtil.intValueOfOptional("8"));
    }

    @Test
    public void testIntValueOfWithNullLongString() {
        assertEquals(Optional.of(Integer.valueOf(1234567890)), StringUtil.intValueOfOptional("1234567890"));
    }

    @Test
    public void testIntValueOfWithNullStartWithZeros() {
        assertEquals(Optional.of(Integer.valueOf(1234)), StringUtil.intValueOfOptional("001234"));
    }

    @Test
    public void testIntValueOfWithNullExceptionIfStringContainsLetter() {
        assertEquals(Optional.empty(), StringUtil.intValueOfOptional("12A2"));
    }

    @Test
    public void testIntValueOfWithNullExceptionIfStringNull() {
        assertEquals(Optional.empty(), StringUtil.intValueOfOptional(null));
    }

    @Test
    public void testIntValueOfWithNullExceptionfIfStringEmpty() {
        assertEquals(Optional.empty(), StringUtil.intValueOfOptional(""));
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
        assertEquals("Hallo Arger", StringUtil.replaceSpecialCharacters("Hallo Arger"));
        assertEquals("aaAeoeeee", StringUtil.replaceSpecialCharacters("åÄöéèë"));
    }

    @Test
    public void testRepeatSpaces() {
        assertEquals("", StringUtil.repeatSpaces(0));
        assertEquals(" ", StringUtil.repeatSpaces(1));
        assertEquals("       ", StringUtil.repeatSpaces(7));
    }

    @Test
    public void testRepeat() {
        assertEquals("", StringUtil.repeat(0, 'a'));
        assertEquals("a", StringUtil.repeat(1, 'a'));
        assertEquals("aaaaaaa", StringUtil.repeat(7, 'a'));
    }

    @Test
    public void testBoldHTML() {
        assertEquals("<b>AA</b>", StringUtil.boldHTML("AA"));
    }

    @Test
    public void testBoldHTMLReturnsOriginalTextIfNonNull() {
        assertEquals("<b>AA</b>", StringUtil.boldHTML("AA", "BB"));
    }

    @Test
    public void testBoldHTMLReturnsAlternativeTextIfNull() {
        assertEquals("<b>BB</b>", StringUtil.boldHTML(null, "BB"));
    }

    @Test
    public void testUnquote() {
        assertEquals("a:", StringUtil.unquote("a::", ':'));
        assertEquals("a:;", StringUtil.unquote("a:::;", ':'));
        assertEquals("a:b%c;", StringUtil.unquote("a::b:%c:;", ':'));
    }

    @Test
    public void testCapitalizeFirst() {
        assertEquals("", StringUtil.capitalizeFirst(""));
        assertEquals("Hello world", StringUtil.capitalizeFirst("Hello World"));
        assertEquals("A", StringUtil.capitalizeFirst("a"));
        assertEquals("Aa", StringUtil.capitalizeFirst("AA"));
    }
}
