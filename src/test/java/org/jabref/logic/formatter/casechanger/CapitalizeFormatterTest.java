package org.jabref.logic.formatter.casechanger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class CapitalizeFormatterTest {

    private CapitalizeFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new CapitalizeFormatter();
    }

    @Test
    public void test() {
        assertEquals("Upper Each First", formatter.format("upper each First"));
        assertEquals("Upper Each First {NOT} {this}", formatter.format("upper each first {NOT} {this}"));
        assertEquals("Upper Each First {N}ot {t}his", formatter.format("upper each first {N}OT {t}his"));
    }

    @Test
    public void formatExample() {
        assertEquals("I Have {a} Dream", formatter.format(formatter.getExampleInput()));
    }

    @ParameterizedTest(name = "input={0}, formattedStr={1}")
    @CsvSource({
            "{}, {}", // {}
            "{upper, {upper", // unmatched braces
            "upper, Upper", // single word lower case
            "Upper, Upper", // single word correct
            "UPPER, Upper", // single word upper case
            "upper each first, Upper Each First", // multiple words lower case
            "Upper Each First, Upper Each First", // multiple words correct
            "UPPER EACH FIRST, Upper Each First", // multiple words upper case
            "{u}pp{e}r, {u}pp{e}r", // single word lower case with {}
            "{U}pp{e}r, {U}pp{e}r", // single word correct with {}
            "{U}PP{E}R, {U}pp{E}r", // single word upper case with {}
            "upper each {NOT} first, Upper Each {NOT} First", // multiple words lower case with {}
            "Upper {E}ach {NOT} First, Upper {E}ach {NOT} First", // multiple words correct with {}
            "UPPER {E}ACH {NOT} FIRST, Upper {E}ach {NOT} First", // multiple words upper case with {}

    })
    public void testInputs(String input, String expectedResult) {
        String formattedStr = formatter.format(input);
        assertEquals(expectedResult, formattedStr);
    }
}
