package org.jabref.logic.util.strings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.util.Pair;

import org.jabref.logic.os.OS;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilTest {

    @Test
    void StringUtilClassIsSmall() throws IOException {
        Path path = Path.of("src", "main", "java", StringUtil.class.getName().replace('.', '/') + ".java");
        int lineCount = Files.readAllLines(path, StandardCharsets.UTF_8).size();

        assertTrue(lineCount <= 830, "StringUtil increased in size to " + lineCount + ". "
                + "We try to keep this class as small as possible. "
                + "Thus think twice if you add something to StringUtil.");
    }

    @Test
    void booleanToBinaryString() {
        assertEquals("0", StringUtil.booleanToBinaryString(false));
        assertEquals("1", StringUtil.booleanToBinaryString(true));
    }

    @Test
    void quoteSimple() {
        assertEquals("a::", StringUtil.quote("a:", "", ':'));
    }

    @Test
    void quoteNullQuotation() {
        assertEquals("a::", StringUtil.quote("a:", null, ':'));
    }

    @Test
    void quoteNullString() {
        assertEquals("", StringUtil.quote(null, ";", ':'));
    }

    @Test
    void quoteQuotationCharacter() {
        assertEquals("a:::;", StringUtil.quote("a:;", ";", ':'));
    }

    @Test
    void quoteMoreComplicated() {
        assertEquals("a::b:%c:;", StringUtil.quote("a:b%c;", "%;", ':'));
    }

    @Test
    void unifyLineBreaks() {
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
    void getCorrectFileName() {
        assertEquals("aa.bib", StringUtil.getCorrectFileName("aa", "bib"));
        assertEquals(".login.bib", StringUtil.getCorrectFileName(".login", "bib"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "bib"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "BIB"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a", "bib"));
        assertEquals("a.bb", StringUtil.getCorrectFileName("a.bb", "bib"));
        assertEquals("", StringUtil.getCorrectFileName(null, "bib"));
    }

    @Test
    void quoteForHTML() {
        assertEquals("&#33;", StringUtil.quoteForHTML("!"));
        assertEquals("&#33;&#33;&#33;", StringUtil.quoteForHTML("!!!"));
    }

    @Test
    void removeBracesAroundCapitals() {
        assertEquals("ABC", StringUtil.removeBracesAroundCapitals("{ABC}"));
        assertEquals("ABC", StringUtil.removeBracesAroundCapitals("{{ABC}}"));
        assertEquals("{abc}", StringUtil.removeBracesAroundCapitals("{abc}"));
        assertEquals("ABCDEF", StringUtil.removeBracesAroundCapitals("{ABC}{DEF}"));
    }

    @Test
    void putBracesAroundCapitals() {
        assertEquals("{ABC}", StringUtil.putBracesAroundCapitals("ABC"));
        assertEquals("{ABC}", StringUtil.putBracesAroundCapitals("{ABC}"));
        assertEquals("abc", StringUtil.putBracesAroundCapitals("abc"));
        assertEquals("#ABC#", StringUtil.putBracesAroundCapitals("#ABC#"));
        assertEquals("{ABC} def {EFG}", StringUtil.putBracesAroundCapitals("ABC def EFG"));
    }

    @Test
    void shaveString() {
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
    void join() {
        String[] s = {"ab", "cd", "ed"};
        assertEquals("ab\\cd\\ed", StringUtil.join(s, "\\", 0, s.length));

        assertEquals("cd\\ed", StringUtil.join(s, "\\", 1, s.length));

        assertEquals("ed", StringUtil.join(s, "\\", 2, s.length));

        assertEquals("", StringUtil.join(s, "\\", 3, s.length));

        assertEquals("", StringUtil.join(new String[] {}, "\\", 0, 0));
    }

    @Test
    void stripBrackets() {
        assertEquals("foo", StringUtil.stripBrackets("[foo]"));
        assertEquals("[foo]", StringUtil.stripBrackets("[[foo]]"));
        assertEquals("", StringUtil.stripBrackets(""));
        assertEquals("[foo", StringUtil.stripBrackets("[foo"));
        assertEquals("]", StringUtil.stripBrackets("]"));
        assertEquals("", StringUtil.stripBrackets("[]"));
        assertEquals("f[]f", StringUtil.stripBrackets("f[]f"));
        assertNull(StringUtil.stripBrackets(null));
    }

    @Test
    void getPart() {
        // Get word between braces
        assertEquals("{makes}", StringUtil.getPart("Practice {makes} perfect", 8, false));
        // When the string is empty and start Index equal zero
        assertEquals("", StringUtil.getPart("", 0, false));
        // When the word are in between close curly bracket
        assertEquals("", StringUtil.getPart("A closed mouth catches no }flies}", 25, false));
        // Get the word from the end of the sentence
        assertEquals("bite", StringUtil.getPart("Barking dogs seldom bite", 19, true));
    }

    @Test
    void findEncodingsForString() {
        // Unused in JabRef, but should be added in case it finds some use
    }

    @Test
    void wrap() {
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
    void decodeStringDoubleArray() {
        assertArrayEquals(new String[][] {{"a", "b"}, {"c", "d"}}, StringUtil.decodeStringDoubleArray("a:b;c:d"));
        assertArrayEquals(new String[][] {{"a", ""}, {"c", "d"}}, StringUtil.decodeStringDoubleArray("a:;c:d"));
        // arrays first differed at element [0][1]; expected: null<null> but was: java.lang.String<null>
        // assertArrayEquals(stringArray2res, StringUtil.decodeStringDoubleArray(encStringArray2));
        assertArrayEquals(new String[][] {{"a", ":b"}, {"c;", "d"}}, StringUtil.decodeStringDoubleArray("a:\\:b;c\\;:d"));
    }

    @Test
    void isInCurlyBrackets() {
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
    void isInSquareBrackets() {
        assertFalse(StringUtil.isInSquareBrackets(""));
        assertFalse(StringUtil.isInSquareBrackets(null));
        assertTrue(StringUtil.isInSquareBrackets("[]"));
        assertTrue(StringUtil.isInSquareBrackets("[a]"));
        assertFalse(StringUtil.isInSquareBrackets("["));
        assertFalse(StringUtil.isInSquareBrackets("]"));
        assertFalse(StringUtil.isInSquareBrackets("a[]a"));
    }

    @Test
    void isInCitationMarks() {
        assertFalse(StringUtil.isInCitationMarks(""));
        assertFalse(StringUtil.isInCitationMarks(null));
        assertTrue(StringUtil.isInCitationMarks("\"\""));
        assertTrue(StringUtil.isInCitationMarks("\"a\""));
        assertFalse(StringUtil.isInCitationMarks("\""));
        assertFalse(StringUtil.isInCitationMarks("a\"\"a"));
    }

    @Test
    void intValueOfSingleDigit() {
        assertEquals(1, StringUtil.intValueOf("1"));
        assertEquals(2, StringUtil.intValueOf("2"));
        assertEquals(8, StringUtil.intValueOf("8"));
    }

    @Test
    void intValueOfLongString() {
        assertEquals(1234567890, StringUtil.intValueOf("1234567890"));
    }

    @Test
    void intValueOfStartWithZeros() {
        assertEquals(1234, StringUtil.intValueOf("001234"));
    }

    @Test
    void intValueOfExceptionIfStringContainsLetter() {
        assertThrows(NumberFormatException.class, () -> StringUtil.intValueOf("12A2"));
    }

    @Test
    void intValueOfExceptionIfStringNull() {
        assertThrows(NumberFormatException.class, () -> StringUtil.intValueOf(null));
    }

    @Test
    void intValueOfExceptionfIfStringEmpty() {
        assertThrows(NumberFormatException.class, () -> StringUtil.intValueOf(""));
    }

    @Test
    void intValueOfWithNullSingleDigit() {
        assertEquals(Optional.of(1), StringUtil.intValueOfOptional("1"));
        assertEquals(Optional.of(2), StringUtil.intValueOfOptional("2"));
        assertEquals(Optional.of(8), StringUtil.intValueOfOptional("8"));
    }

    @Test
    void intValueOfWithNullLongString() {
        assertEquals(Optional.of(1234567890), StringUtil.intValueOfOptional("1234567890"));
    }

    @Test
    void intValueOfWithNullStartWithZeros() {
        assertEquals(Optional.of(1234), StringUtil.intValueOfOptional("001234"));
    }

    @Test
    void intValueOfWithNullExceptionIfStringContainsLetter() {
        assertEquals(Optional.empty(), StringUtil.intValueOfOptional("12A2"));
    }

    @Test
    void intValueOfWithNullExceptionIfStringNull() {
        assertEquals(Optional.empty(), StringUtil.intValueOfOptional(null));
    }

    @Test
    void intValueOfWithNullExceptionfIfStringEmpty() {
        assertEquals(Optional.empty(), StringUtil.intValueOfOptional(""));
    }

    @ParameterizedTest
    @CsvSource({
            "'Test', 'Test', 20",
            "'...', 'Test', 3",
            "'TestTes...', 'TestTestTestTestTest', 10",
            "'', , 10"
    })
    void limitStringLength(String expected, String input, int maxLength) {
        assertEquals(expected, StringUtil.limitStringLength(input, maxLength));
    }

    @Test
    void limitStringLengthLimiting() {
        assertEquals(10, StringUtil.limitStringLength("TestTestTestTestTest", 10).length());
    }

    @Test
    void replaceSpecialCharacters() {
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
    void repeatSpaces(String result, int count) {
        assertEquals(result, StringUtil.repeatSpaces(count));
    }

    @Test
    void repeat() {
        assertEquals("", StringUtil.repeat(0, 'a'));
        assertEquals("a", StringUtil.repeat(1, 'a'));
        assertEquals("aaaaaaa", StringUtil.repeat(7, 'a'));
    }

    @Test
    void boldHTML() {
        assertEquals("<b>AA</b>", StringUtil.boldHTML("AA"));
    }

    @Test
    void boldHTMLReturnsOriginalTextIfNonNull() {
        assertEquals("<b>AA</b>", StringUtil.boldHTML("AA", "BB"));
    }

    @Test
    void boldHTMLReturnsAlternativeTextIfNull() {
        assertEquals("<b>BB</b>", StringUtil.boldHTML(null, "BB"));
    }

    @Test
    void unquote() {
        assertEquals("a:", StringUtil.unquote("a::", ':'));
        assertEquals("a:;", StringUtil.unquote("a:::;", ':'));
        assertEquals("a:b%c;", StringUtil.unquote("a::b:%c:;", ':'));
    }

    @Test
    void capitalizeFirst() {
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
    void guoteStringIfSpaceIsContained(String expected, String source) {
        assertEquals(expected, StringUtil.quoteStringIfSpaceIsContained(source));
    }

    @Test
    void stripAccents() {
        assertEquals("aAoeee", StringUtil.stripAccents("åÄöéèë"));
        assertEquals("Muhlbach", StringUtil.stripAccents("Mühlbach"));
    }

    static Stream<Arguments> containsWhitespace() {
        return Stream.of(
                Arguments.of(true, "file url"),
                Arguments.of(true, "file\nurl"),
                Arguments.of(true, "file\r\nurl"),
                Arguments.of(true, "file\rurl"),
                Arguments.of(true, "file\furl"),
                Arguments.of(true, "file_url "),
                Arguments.of(true, "file url\n"),
                Arguments.of(true, " "),

                Arguments.of(false, "file_url"),
                Arguments.of(false, "PascalCase"),
                Arguments.of(false, ""));
    }

    @ParameterizedTest
    @MethodSource
    void containsWhitespace(Boolean expected, String input) {
        assertEquals(expected, StringUtil.containsWhitespace(input));
    }

    @Test
    void alignStringTable() {
        List<Pair<String, String>> given = List.of(
                new Pair<>("Apple", "Slice"),
                new Pair<>("Bread", "Loaf"),
                new Pair<>("Paper", "Sheet"),
                new Pair<>("Country", "County"));

        String expected = """
                Apple   : Slice
                Bread   : Loaf
                Paper   : Sheet
                Country : County
                """.replace("\n", OS.NEWLINE);

        assertEquals(expected, StringUtil.alignStringTable(given));
    }
}
