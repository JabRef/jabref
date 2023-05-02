package org.jabref.logic.formatter.bibtexfields;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeDollarSignFormatterTest {

    private EscapeDollarSignFormatter formatter = new EscapeDollarSignFormatter();

    private static Stream<Arguments> correctlyFormats() {
        return Stream.of(
                // formatReturnsSameTextIfNoDollarSignPresent
                Arguments.of("Lorem ipsum", "Lorem ipsum"),
                // formatEscapesDollarSignIfPresent
                Arguments.of("Lorem\\$ipsum", "Lorem$ipsum"),
                // keeps escapings
                Arguments.of("Lorem\\$ipsum", "Lorem\\$ipsum"),
                Arguments.of("Dollar sign: 1x: \\$ 2x: \\$\\$ 3x: \\$\\$\\$", "Dollar sign: 1x: \\$ 2x: \\$\\$ 3x: \\$\\$\\$"),
                // mixed field
                Arguments.of("Dollar sign: 1x: \\$ 2x: \\$\\$ 3x: \\$\\$\\$", "Dollar sign: 1x: $ 2x: $$ 3x: $$$")
        );
    }

    @ParameterizedTest
    @MethodSource
    void correctlyFormats(String expected, String input) throws Exception {
        assertEquals(expected, formatter.format(input));
    }

    @Test
    void formatExample() {
        assertEquals("Text\\$with\\$dollar\\$sign", formatter.format(formatter.getExampleInput()));
    }
}
