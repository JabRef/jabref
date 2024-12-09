package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NormalizeUnicodeFormatterTest {

    private NormalizeUnicodeFormatter formatter = new NormalizeUnicodeFormatter();

    @ParameterizedTest
    @CsvSource({
            "John, John",
            "\u00C5, \u0041\u030A"
    })
    void format(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
