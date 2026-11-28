package org.jabref.logic.util.strings;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RomanNumeralParserTest {
    @ParameterizedTest
    @CsvSource(textBlock = """
            V,       5
            IX,      9
            ix,      9
            ' IX ',  9
            XIV,     14
            MCMXCIV, 1994
            """)
    void parseValidRomanNumerals(String input, int expected) {
        assertEquals(Optional.of(expected), RomanNumeralParser.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"IIII", "VX", "ABC", "IC", "", "   "})
    void parseInvalidReturnsEmpty(String input) {
        assertEquals(Optional.empty(), RomanNumeralParser.parse(input));
    }

    @Test
    void parseNullReturnsEmpty() {
        assertEquals(Optional.empty(), RomanNumeralParser.parse(null));
    }
}
