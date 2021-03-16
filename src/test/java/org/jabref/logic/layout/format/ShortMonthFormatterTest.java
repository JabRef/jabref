package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShortMonthFormatterTest {

    private LayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new ShortMonthFormatter();
    }

    @ParameterizedTest(name = "formattedStr={0}, input={1}")
    @CsvSource({
            "jan, 1", // jan
            "feb, 2", // feb
            "mar, 3", // mar
            "apr, 4", // apr
            "may, 5", // may
            "jun, 6", // jun
            "jul, 7", // jul
            "aug, 8", // aug
            "sep, 9", // sep
            "oct, 10", // oct
            "nov, 11", // nov
            "dec, 12", // dec
            "jan, Januar" // jan
    })
    public void testValidFormat(String expectedResult, String input) {
        assertEquals(expectedResult, formatter.format(input));
    }

    @ParameterizedTest(name = "formattedStr={0}, input={1}")
    @CsvSource({
            "'', -1", // -1
            "'', 0", // 0
            "'', 13", // 13
            "'', abc", // abc
    })
    public void testInvalidFormat(String expectedResult, String input) {
        assertEquals(expectedResult, formatter.format(input));
    }
}
