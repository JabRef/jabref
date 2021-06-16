package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateFormatterTest {

    private ParamLayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new DateFormatter();
    }

    @Test
    public void testDefaultFormat() {
        assertEquals("2016-07-15", formatter.format("2016-07-15"));
    }

    @Test
    public void testRequestedFormat() {
        formatter.setArgument("MM/yyyy");
        assertEquals("07/2016", formatter.format("2016-07-15"));
    }

    @ParameterizedTest(name = "formatArg={0}, input={1}, formattedStr={2}")
    @CsvSource({
            "MM/dd/yyyy, 2016-07-15, 07/15/2016", // MM/dd/yyyy
            "dd MMMM yyyy, 2016-07-15, 15 July 2016", // dd MMMM yyyy
            "MM-dd-yyyy, 2016-07-15, 07-15-2016", // MM-dd-yyyy
            "yyyy.MM.dd, 2016-07-15, 2016.07.15", // yyyy.MM.dd
            "yyyy/MM, 2016-07-15, 2016/07", // yyyy/MM
    })
    public void testOtherFormats(String formatArg, String input, String expectedResult) {
        formatter.setArgument(formatArg);
        String formattedStr = formatter.format(input);
        assertEquals(expectedResult, formattedStr);
    }
}
