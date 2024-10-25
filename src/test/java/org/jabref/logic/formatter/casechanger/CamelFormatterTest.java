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
class CamelFormatterTest {

    private CamelFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new CamelFormatter();
    }

    @ParameterizedTest
    @MethodSource("provideStringsForFormat")
    void test(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    private static Stream<Arguments> provideStringsForFormat() {
        return Stream.of(
                Arguments.of("CamelTitleFormatter", "Camel Title Formatter"),
                Arguments.of("CamelTitleFormatter", "CAMEL TITLE FORMATTER"),
                Arguments.of("CamelTitleFormatter", "camel title formatter"),
                Arguments.of("CamelTitleFormatter", "cAMEL tITLE fORMATTER"),
                Arguments.of("C", "c"));
    }

    @Test
    void formatExample() {
        assertEquals("ThisIsExampleInput", formatter.format(formatter.getExampleInput()));
    }
}
