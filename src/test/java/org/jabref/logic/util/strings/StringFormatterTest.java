package org.jabref.logic.util.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringFormatterTest {

    @Test
    void formatErrorMessage() {
        String expectedError = "The server time zone value 'MSK' is unrecognized or represents more than one time zone. You must configure either the server or JDBC driver (via \n" +
                "the serverTimezone configuration property) to use a more specifc time zone value if you want to utilize time zone support.";
        StringFormatter stringFormatter = new StringFormatter();
        String error = "The server time zone value 'MSK' is unrecognized or represents more than one time zone. You must configure either the server or JDBC driver (via the serverTimezone configuration property) to use a more specifc time zone value if you want to utilize time zone support.";
        Exception exceptionUnderTest = new Exception(error);
        String errorToBeThrown = stringFormatter.formatErrorMessage(exceptionUnderTest);
        String[] lines = errorToBeThrown.split(System.lineSeparator());
        assertEquals(expectedError, errorToBeThrown);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String[] split = line.split("\\s+");
            if (i != lines.length - 1) {
                assertEquals(25, split.length);
            }
        }

    }
}
