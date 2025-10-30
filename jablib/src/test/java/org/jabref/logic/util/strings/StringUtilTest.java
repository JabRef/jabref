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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilTest {

    @Test
    void StringUtilClassIsSmall() throws IOException {
        Path path = Path.of("src", "main", "java",
                StringUtil.class.getName().replace('.', '/') + ".java");
        int lineCount = Files.readAllLines(path, StandardCharsets.UTF_8).size();

        assertTrue(lineCount <= 830, "StringUtil increased in size to " + lineCount + ". "
                + "We try to keep this class as small as possible. "
                + "Thus think twice if you add something to StringUtil.");
    }

    @ParameterizedTest
    @CsvSource({
            "false, 0",
            "true, 1",
    })
    void booleanToBinaryString(boolean input, String expected) {
        assertEquals(expected, StringUtil.booleanToBinaryString(input));
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

    @ParameterizedTest
    @CsvSource({
            // Mac < v9
            "'\r',newline",
            // Windows
            "'\r\n','newline'",
            // Unix
            "'\n', 'newline'"
    })
    void unifyLineBreaks(String input, String expected) {
        assertEquals(expected, StringUtil.unifyLineBreaks(input, expected));
    }

    @ParameterizedTest
    @CsvSource({
            "aa,bib,aa.bib",
            ".login,bib,.login.bib",
            "a.bib,bib,a.bib",
            "a.bib,BIB,a.bib",
            "a,bib,a.bib",
            "a.bb,bib,a.bb",
            ",bib,''"
    })
    void getCorrectFileName(String filename, String extension, String expected) {
        assertEquals(expected, StringUtil.getCorrectFileName(filename, extension));
    }

    @ParameterizedTest
    @CsvSource({
            "&#33;,!",
            "&#33;&#33;&#33;,!!!"
    })
    void quoteForHTML(String expected, String input) {
        assertEquals(expected, StringUtil.quoteForHTML(input));
    }

    @ParameterizedTest
    @CsvSource({
            "ABC,{ABC}",
            "ABC,{{ABC}}",
            "{abc},{abc}",
            "ABCDEF,{ABC}{DEF}"
    })
    void removeBracesAroundCapitals(String expected, String input) {
        assertEquals(expected, StringUtil.removeBracesAroundCapitals(input));
    }

    @ParameterizedTest
    @CsvSource({
            "ABC,{ABC}",
            "{ABC},{ABC}",
            "abc,abc",
            "#ABC#,#ABC#",
            "ABC def EFG,{ABC} def {EFG}"
    })
    void putBracesAroundCapitals(String input, String expected) {
        assertEquals(expected, StringUtil.putBracesAroundCapitals(input));
    }

    @ParameterizedTest
    @CsvSource({
            "'',''",
            "'   aaa\t\t\n\r','aaa'",
            "'  {a}    ','a'",
            "'  \"a\"    ','a'",
            "'  {{a}}    ','{a}'",
            "'  \"{a}\"    ','{a}'",
            "'  \"{a\"}    ','\"{a\"}'"
    })
    void shaveString(String input, String expected) {
        assertEquals(expected, StringUtil.shaveString(input));
    }

    @Test
    void shaveStringReturnsEmptyWhenNull() {
        assertEquals("", StringUtil.shaveString(null));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            'ab\\cd\\ed', 'ab;cd;ed', 0, 3
            'cd\\ed',     'ab;cd;ed', 1, 3
            'ed',         'ab;cd;ed', 2, 3
            '',           'ab;cd;ed', 3, 3
            '',           '',         0, 0
            """)
    void join(String expected, String arrayStr, int from, int to) {
        String[] array = arrayStr.split(";");
        assertEquals(expected, StringUtil.join(array, "\\", from, to));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock = """
            EXPECTED, INPUT
            foo,      '[foo]'
            '[foo]',  '[[foo]]'
            '',       ''
            '[foo',   '[foo'
            ']',      ']'
            '',       '[]'
            'f[]f',   'f[]f'
            """)
    void stripBrackets(String expected, String input) {
        assertEquals(expected, StringUtil.stripBrackets(input));
    }

    @ParameterizedTest
    @CsvSource({
            // Get word between braces
            "'{makes}','Practice {makes} perfect',8,false",
            // When the string is empty and start Index equal zero
            "'','',0,false",
            // When the word are in between close curly bracket
            "'','A closed mouth catches no }flies}',25,false",
            // Get the word from the end of the sentence
            "'bite','Barking dogs seldom bite',19,true"
    })
    void getPart(String expected, String input, int startIndex, boolean forward) {
        assertEquals(expected, StringUtil.getPart(input, startIndex, forward));
    }

    @Test
    void findEncodingsForString() {
        // Unused in JabRef, but should be added in case it finds some use
    }

    @ParameterizedTest
    @CsvSource({
            "'aaaaa bbbbb ccccc',5,'aaaaanewline\tbbbbbnewline\tccccc'",
            "'aaaaa bbbbb ccccc',8,'aaaaa bbbbbnewline\tccccc'",
            "'aaaaa bbbbb ccccc',11,'aaaaa bbbbbnewline\tccccc'",
            "'aaaaa bbbbb ccccc',12,'aaaaa bbbbb ccccc'",
            "'aaaaa\nbbbbb\nccccc',12,'aaaaanewline\tnewline\tbbbbbnewline\tnewline\tccccc'",
            "'aaaaa\n\nbbbbb\nccccc',12,'aaaaanewline\tnewline\tnewline\tbbbbbnewline\tnewline\tccccc'",
            "'aaaaa\r\nbbbbb\r\nccccc',12,'aaaaanewline\tnewline\tbbbbbnewline\tnewline\tccccc'"
    })
    void wrap(String input, int wrapLength, String expected) {
        String newline = "newline";
        assertEquals(expected, StringUtil.wrap(input, wrapLength, newline));
    }

    @ParameterizedTest
    @MethodSource("provideDecodeStringDoubleArrayData")
    void decodeStringDoubleArray(String input, String[][] expected) {
        assertArrayEquals(expected, StringUtil.decodeStringDoubleArray(input));
    }

    static Stream<Arguments> provideDecodeStringDoubleArrayData() {
        return Stream.of(
                Arguments.of("a:b;c:d", new String[][] {{"a", "b"}, {"c", "d"}}),
                Arguments.of("a:;c:d", new String[][] {{"a", ""}, {"c", "d"}}),
                Arguments.of("a:\\:b;c\\;:d", new String[][] {{"a", ":b"}, {"c;", "d"}})
        );
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            false,
            true, {}
            true, {a}
            true, '{a{a}}'
            true, '{{\\AA}sa {\\AA}Stor{\\aa}}'
            false, {
            false, }
            false, a{}a
            false, '{\\AA}sa {\\AA}Stor{\\aa}'
            """)
    void isInCurlyBrackets(boolean expected, String input) {
        assertEquals(expected, StringUtil.isInCurlyBrackets(input));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            false,
            true, []
            true, [a]
            false, [
            false, ]
            false, a[]a
            """)
    void isInSquareBrackets(boolean expected, String input) {
        assertEquals(expected, StringUtil.isInSquareBrackets(input));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            false,
            true, ""
            true, "a"
            false, "
            false, a""a
            """)
    void isInCitationMarks(boolean expected, String input) {
        assertEquals(expected, StringUtil.isInCitationMarks(input));
    }

    @ParameterizedTest
    @CsvSource({
            "1,'1'",
            "2,'2'",
            "8,'8'"
    })
    void intValueOfSingleDigit(int expected, String input) {
        assertEquals(expected, StringUtil.intValueOf(input));
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

    @ParameterizedTest
    @CsvSource({
            "1,'1'",
            "2,'2'",
            "8,'8'"
    })
    void intValueOfWithNullSingleDigit(int expected, String input) {
        assertEquals(Optional.of(expected), StringUtil.intValueOfOptional(input));
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

    @ParameterizedTest
    @CsvSource({
            "'Hallo Arger','Hallo Arger'",
            "'aaAeoeeee','åÄöéèë'"
    })
    void replaceSpecialCharacters(String expected, String input) {
        assertEquals(expected, StringUtil.replaceSpecialCharacters(input));
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

    @ParameterizedTest
    @CsvSource({
            "'',0,'a'",
            "'a',1,'a'",
            "'aaaaaaa',7,'a'"
    })
    void repeat(String expected, int count, char character) {
        assertEquals(expected, StringUtil.repeat(count, character));
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

    @ParameterizedTest
    @CsvSource({
            "'a:','a::',':'",
            "'a:;','a:::;',':'",
            "'a:b%c;','a::b:%c:;',':'"
    })
    void unquote(String expected, String input, char quoteChar) {
        assertEquals(expected, StringUtil.unquote(input, quoteChar));
    }

    @ParameterizedTest
    @CsvSource({
            "'',''",
            "'Hello world','Hello World'",
            "'A','a'",
            "'Aa','AA'"
    })
    void capitalizeFirst(String expected, String input) {
        assertEquals(expected, StringUtil.capitalizeFirst(input));
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
    void quoteStringIfSpaceIsContained(String expected, String source) {
        assertEquals(expected, StringUtil.quoteStringIfSpaceIsContained(source));
    }

    @ParameterizedTest
    @CsvSource({
            "aAoeee,åÄöéèë",
            "Muhlbach,Mühlbach"
    })
    void stripAccents(String expected, String input) {
        assertEquals(expected, StringUtil.stripAccents(input));
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
