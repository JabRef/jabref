package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RisMonthTest {

    @Test
    public void testEmpty() {
        assertEquals("", new RisMonth().format(""));
    }

    @Test
    public void testNull() {
        assertEquals("", new RisMonth().format(null));
    }

    @ParameterizedTest(name = "input={0}, formattedStr={1}")
    @CsvSource({
            "jan, 01", // jan
            "feb, 02", // feb
            "mar, 03", // mar
            "apr, 04", // apr
            "may, 05", // may
            "jun, 06", // jun
            "jul, 07", // jul
            "aug, 08", // aug
            "sep, 09", // sep
            "oct, 10", // oct
            "nov, 11", // nov
            "dec, 12", // dec
    })
    public void testValidMonth(String input, String expectedResult) {
        String formattedStr = new RisMonth().format(input);
        assertEquals(expectedResult, formattedStr);
    }

    @Test
    public void testInvalidMonth() {
        assertEquals("abcd", new RisMonth().format("abcd"));
    }
}
