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
class ShortTitleFormatterTest {

    private ShortTitleFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ShortTitleFormatter();
    }

    @ParameterizedTest
    @MethodSource("provideStringsForFormat")
    void test(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    private static Stream<Arguments> provideStringsForFormat() {
        return Stream.of(
                Arguments.of("Very short title", "Very short title"),
                Arguments.of("This is very", "This is very short title"),
                Arguments.of("This is", "This is"),
                Arguments.of("This", "This"),
                Arguments.of("", ""),
                Arguments.of("very short title", "A very short title"));
    }

    @Test
    void formatExample() {
        assertEquals("This is short", formatter.format(formatter.getExampleInput()));
    }
}
