package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShortMonthFormatterTest {

    private LayoutFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ShortMonthFormatter();
    }

    @Test
    void formatNullInput() {
        assertEquals("", formatter.format(null));
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void formatDifferentInputs(String formattedString, String originalString) {
        assertEquals(formattedString, formatter.format(originalString));
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
                Arguments.of("jan", "jan"),
                Arguments.of("jan", "January"),
                Arguments.of("jan", "Januar"),
                Arguments.of("jan", "01"),
                Arguments.of("", "Invented Month"),
                Arguments.of("", "")
        );
    }
}
