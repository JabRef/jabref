package org.jabref.logic.formatter.casechanger;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class CamelNFormatterTest {

    private CamelNFormatter formatter_3;
    private CamelNFormatter formatter_1;
    private CamelNFormatter formatter_0;

    @BeforeEach
    void setUp() {
        formatter_3 = new CamelNFormatter(3);
        formatter_1 = new CamelNFormatter(1);
        formatter_0 = new CamelNFormatter(0);
    }

    @ParameterizedTest
    @MethodSource("provideStringsForFormat_3")
    void test_3(String expected, String input) {
        assertEquals(expected, formatter_3.format(input));
    }

    @ParameterizedTest
    @MethodSource("provideStringsForFormat_1")
    void test_1(String expected, String input) {
        assertEquals(expected, formatter_1.format(input));
    }

    @ParameterizedTest
    @MethodSource("provideStringsForFormat_0")
    void test_0(String expected, String input) {
        assertEquals(expected, formatter_0.format(input));
    }

    private static Stream<Arguments> provideStringsForFormat_3() {
        return Stream.of(
                Arguments.of("CamelTitleFormatter", "Camel Title Formatter"),
                Arguments.of("CamelTitleFormatter", "CAMEL TITLE FORMATTER"),
                Arguments.of("CamelTitleFormatter", "camel title formatter"),
                Arguments.of("CamelTitleFormatter", "cAMEL tITLE fORMATTER"),
                Arguments.of("CamelTitleFormatter", "cAMEL tITLE fORMATTER test"),
                Arguments.of("CamelTitleFormatter", "cAMEL tITLE fORMATTER tEST"),
                Arguments.of("CamelTitleFormatter", "camel title formatter test"),
                Arguments.of("", ""),
                Arguments.of("C", "c"));
    }

    private static Stream<Arguments> provideStringsForFormat_1() {
        return Stream.of(
                Arguments.of("Camel", "Camel Title Formatter"),
                Arguments.of("Camel", "CAMEL TITLE FORMATTER"),
                Arguments.of("Camel", "camel title formatter"),
                Arguments.of("Camel", "cAMEL tITLE fORMATTER"),
                Arguments.of("", ""),
                Arguments.of("C", "c"));
    }

    private static Stream<Arguments> provideStringsForFormat_0() {
        return Stream.of(
                Arguments.of("", "Camel Title Formatter"),
                Arguments.of("", ""));
    }

    @Test
    void formatExample_3() {
        assertEquals("ThisIsCamel", formatter_3.format(formatter_3.getExampleInput()));
    }

    @Test
    void formatExample_1() {
        assertEquals("This", formatter_1.format(formatter_1.getExampleInput()));
    }

    @Test
    void formatExample_0() {
        assertEquals("", formatter_0.format(formatter_0.getExampleInput()));
    }
}
