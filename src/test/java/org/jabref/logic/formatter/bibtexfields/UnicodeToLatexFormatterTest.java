package org.jabref.logic.formatter.bibtexfields;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnicodeToLatexFormatterTest {

    private UnicodeToLatexFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new UnicodeToLatexFormatter();
    }

    private static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of("", ""), // empty string input
                Arguments.of("abc", "abc"), // non unicode input
                Arguments.of("\u00E5\u00E4\u00F6", "{{\\aa}}{\\\"{a}}{\\\"{o}}"), // multiple unicodes input
                Arguments.of("\u0081", ""), // high code point unicode, boundary case: cp = 129
                Arguments.of("\u0080", ""), // high code point unicode, boundary case: cp = 128 < 129
                Arguments.of(new UnicodeToLatexFormatter().getExampleInput(), "M{\\\"{o}}nch")
        );
    }

    @ParameterizedTest()
    @MethodSource("testCases")
    void testFormat(String input, String expectedResult){
        assertEquals(expectedResult, formatter.format(input));
    }

}
