package org.jabref.model.strings;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilTest {

    @Test
    void StringUtilClassIsSmall() throws Exception {
        Path path = Path.of("src", "main", "java", StringUtil.class.getName().replace('.', '/') + ".java");
        int lineCount = Files.readAllLines(path, StandardCharsets.UTF_8).size();

        assertTrue(lineCount <= 761, "StringUtil increased in size to " + lineCount + ". "
                + "We try to keep this class as small as possible. "
                + "Thus think twice if you add something to StringUtil.");
    }

    @Test
    void testBooleanToBinaryString() {
        assertEquals("0", StringUtil.booleanToBinaryString(false));
        assertEquals("1", StringUtil.booleanToBinaryString(true));
    }

    @Test
    void testQuoteSimple() {
        assertEquals("a::", StringUtil.quote("a:", "", ':'));
    }

    @Test
    void testQuoteNullQuotation() {
        assertEquals("a::", StringUtil.quote("a:", null, ':'));
    }

    @Test
    void testQuoteNullString() {
        assertEquals("", StringUtil.quote(null, ";", ':'));
    }

    @Test
    void testQuoteQuotationCharacter() {
        assertEquals("a:::;", StringUtil.quote("a:;", ";", ':'));
    }

    @Test
    void testQuoteMoreComplicated() {
        assertEquals("a::b:%c:;", StringUtil.quote("a:b%c;", "%;", ':'));
    }

    @Test
    void testUnifyLineBreaks() {
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
    void testGetCorrectFileName() {
        assertEquals("aa.bib", StringUtil.getCorrectFileName("aa", "bib"));
        assertEquals(".login.bib", StringUtil.getCorrectFileName(".login", "bib"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "bib"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "BIB"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a", "bib"));
        assertEquals("a.bb", StringUtil.getCorrectFileName("a.bb", "bib"));
        assertEquals("", StringUtil.getCorrectFileName(null, "bib"));
    }

    @Test
    void testQuoteForHTML() {
        assertEquals("&#33;", StringUtil.quoteForHTML("!"));
        assertEquals("&#33;&#33;&#33;", StringUtil.quoteForHTML("!!!"));
    }

    @Test
    void testRemoveBracesAroundCapitals() {
        assertEquals("ABC", StringUtil.removeBracesAroundCapitals("{ABC}"));
        assertEquals("ABC", StringUtil.removeBracesAroundCapitals("{{ABC}}"));
        assertEquals("{abc}", StringUtil.removeBracesAroundCapitals("{abc}"));
        assertEquals("ABCDEF", StringUtil.removeBracesAroundCapitals("{ABC}{DEF}"));
    }

    @Test
    void testPutBracesAroundCapitals() {
        assertEquals("{ABC}", StringUtil.putBracesAroundCapitals("ABC"));
        assertEquals("{ABC}", StringUtil.putBracesAroundCapitals("{ABC}"));
        assertEquals("abc", StringUtil.putBracesAroundCapitals("abc"));
        assertEquals("#ABC#", StringUtil.putBracesAroundCapitals("#ABC#"));
        assertEquals("{ABC} def {EFG}", StringUtil.putBracesAroundCapitals("ABC def EFG"));
    }

    @Test
    void testShaveString() {

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
    void testJoin() {
        String[] s = {"ab", "cd", "ed"};
        assertEquals("ab\\cd\\ed", StringUtil.join(s, "\\", 0, s.length));

        assertEquals("cd\\ed", StringUtil.join(s, "\\", 1, s.length));

        assertEquals("ed", StringUtil.join(s, "\\", 2, s.length));

        assertEquals("", StringUtil.join(s, "\\", 3, s.length));

        assertEquals("", StringUtil.join(new String[]{}, "\\", 0, 0));
    }

    @Test
    void testStripBrackets() {
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
    void testGetPart() {
        // Should be added
    }

    @Test
    void testFindEncodingsForString() {
        // Unused in JabRef, but should be added in case it finds some use
    }

    @Test
    void testWrap() {
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
    void testDecodeStringDoubleArray() {
        assertArrayEquals(new String[][]{{"a", "b"}, {"c", "d"}}, StringUtil.decodeStringDoubleArray("a:b;c:d"));
        assertArrayEquals(new String[][]{{"a", ""}, {"c", "d"}}, StringUtil.decodeStringDoubleArray("a:;c:d"));
        // arrays first differed at element [0][1]; expected: null<null> but was: java.lang.String<null>
        // assertArrayEquals(stringArray2res, StringUtil.decodeStringDoubleArray(encStringArray2));
        assertArrayEquals(new String[][]{{"a", ":b"}, {"c;", "d"}}, StringUtil.decodeStringDoubleArray("a:\\:b;c\\;:d"));
    }

    @Test
    void testIsInCurlyBrackets() {
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
    void testIsInSquareBrackets() {
        assertFalse(StringUtil.isInSquareBrackets(""));
        assertFalse(StringUtil.isInSquareBrackets(null));
        assertTrue(StringUtil.isInSquareBrackets("[]"));
        assertTrue(StringUtil.isInSquareBrackets("[a]"));
        assertFalse(StringUtil.isInSquareBrackets("["));
        assertFalse(StringUtil.isInSquareBrackets("]"));
        assertFalse(StringUtil.isInSquareBrackets("a[]a"));
    }

    @Test
    void testIsInCitationMarks() {
        assertFalse(StringUtil.isInCitationMarks(""));
        assertFalse(StringUtil.isInCitationMarks(null));
        assertTrue(StringUtil.isInCitationMarks("\"\""));
        assertTrue(StringUtil.isInCitationMarks("\"a\""));
        assertFalse(StringUtil.isInCitationMarks("\""));
        assertFalse(StringUtil.isInCitationMarks("a\"\"a"));
    }

    @Test
    void testIntValueOfSingleDigit() {
        assertEquals(1, StringUtil.intValueOf("1"));
        assertEquals(2, StringUtil.intValueOf("2"));
        assertEquals(8, StringUtil.intValueOf("8"));
    }

    @Test
    void testIntValueOfLongString() {
        assertEquals(1234567890, StringUtil.intValueOf("1234567890"));
    }

    @Test
    void testIntValueOfStartWithZeros() {
        assertEquals(1234, StringUtil.intValueOf("001234"));
    }

    @Test
    void testIntValueOfExceptionIfStringContainsLetter() {
        assertThrows(NumberFormatException.class, () -> StringUtil.intValueOf("12A2"));
    }

    @Test
    void testIntValueOfExceptionIfStringNull() {
        assertThrows(NumberFormatException.class, () -> StringUtil.intValueOf(null));
    }

    @Test
    void testIntValueOfExceptionfIfStringEmpty() {
        assertThrows(NumberFormatException.class, () -> StringUtil.intValueOf(""));
    }

    @Test
    void testIntValueOfWithNullSingleDigit() {
        assertEquals(Optional.of(1), StringUtil.intValueOfOptional("1"));
        assertEquals(Optional.of(2), StringUtil.intValueOfOptional("2"));
        assertEquals(Optional.of(8), StringUtil.intValueOfOptional("8"));
    }

    @Test
    void testIntValueOfWithNullLongString() {
        assertEquals(Optional.of(1234567890), StringUtil.intValueOfOptional("1234567890"));
    }

    @Test
    void testIntValueOfWithNullStartWithZeros() {
        assertEquals(Optional.of(1234), StringUtil.intValueOfOptional("001234"));
    }

    @Test
    void testIntValueOfWithNullExceptionIfStringContainsLetter() {
        assertEquals(Optional.empty(), StringUtil.intValueOfOptional("12A2"));
    }

    @Test
    void testIntValueOfWithNullExceptionIfStringNull() {
        assertEquals(Optional.empty(), StringUtil.intValueOfOptional(null));
    }

    @Test
    void testIntValueOfWithNullExceptionfIfStringEmpty() {
        assertEquals(Optional.empty(), StringUtil.intValueOfOptional(""));
    }

    @Test
    void testLimitStringLengthShort() {
        assertEquals("Test", StringUtil.limitStringLength("Test", 20));
    }

    @Test
    void testLimitStringLengthLimiting() {
        assertEquals("TestTes...", StringUtil.limitStringLength("TestTestTestTestTest", 10));
        assertEquals(10, StringUtil.limitStringLength("TestTestTestTestTest", 10).length());
    }

    @Test
    void testLimitStringLengthNullInput() {
        assertEquals("", StringUtil.limitStringLength(null, 10));
    }

    @Test
    void testReplaceSpecialCharacters() {
        assertEquals("Hallo Arger", StringUtil.replaceSpecialCharacters("Hallo Arger"));
        assertEquals("aaAeoeeee", StringUtil.replaceSpecialCharacters("åÄöéèë"));
    }

    @Test
    void replaceSpecialCharactersWithNonNormalizedUnicode() {
        assertEquals("Modele", StringUtil.replaceSpecialCharacters("Modèle"));
    }

    static Stream<Arguments> testRepeatSpacesData() {
        return Stream.of(
                Arguments.of("", -1),
                Arguments.of("", 0),
                Arguments.of(" ", 1),
                Arguments.of("       ", 7)
        );
    }

    @ParameterizedTest
    @MethodSource("testRepeatSpacesData")
    void testRepeatSpaces(String result, int count) {
        assertEquals(result, StringUtil.repeatSpaces(count));
    }

    @Test
    void testRepeat() {
        assertEquals("", StringUtil.repeat(0, 'a'));
        assertEquals("a", StringUtil.repeat(1, 'a'));
        assertEquals("aaaaaaa", StringUtil.repeat(7, 'a'));
    }

    @Test
    void testBoldHTML() {
        assertEquals("<b>AA</b>", StringUtil.boldHTML("AA"));
    }

    @Test
    void testBoldHTMLReturnsOriginalTextIfNonNull() {
        assertEquals("<b>AA</b>", StringUtil.boldHTML("AA", "BB"));
    }

    @Test
    void testBoldHTMLReturnsAlternativeTextIfNull() {
        assertEquals("<b>BB</b>", StringUtil.boldHTML(null, "BB"));
    }

    @Test
    void testUnquote() {
        assertEquals("a:", StringUtil.unquote("a::", ':'));
        assertEquals("a:;", StringUtil.unquote("a:::;", ':'));
        assertEquals("a:b%c;", StringUtil.unquote("a::b:%c:;", ':'));
    }

    @Test
    void testCapitalizeFirst() {
        assertEquals("", StringUtil.capitalizeFirst(""));
        assertEquals("Hello world", StringUtil.capitalizeFirst("Hello World"));
        assertEquals("A", StringUtil.capitalizeFirst("a"));
        assertEquals("Aa", StringUtil.capitalizeFirst("AA"));
    }

    private static Stream<Arguments> getQuoteStringIfSpaceIsContainedData() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("\" \"", " "),
                Arguments.of("world", "world"),
                Arguments.of("\"hello world\"", "hello world")
        );
    }

    @ParameterizedTest
    @MethodSource("getQuoteStringIfSpaceIsContainedData")
    void testGuoteStringIfSpaceIsContained(String expected, String source) {
        assertEquals(expected, StringUtil.quoteStringIfSpaceIsContained(source));
    }
}
