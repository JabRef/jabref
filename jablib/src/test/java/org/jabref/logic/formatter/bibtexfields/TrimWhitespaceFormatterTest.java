package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrimWhitespaceFormatterTest {

    private TrimWhitespaceFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new TrimWhitespaceFormatter();
    }

    @ParameterizedTest
    @ValueSource(strings = {"\twhitespace", "whitespace\t", "\twhitespace\t\t"})
    void removeHorizontalTabulations(String expression) {
        assertEquals("whitespace", formatter.format(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\nwhitespace", "whitespace\n", "\nwhitespace\n\n"})
    void removeLineFeeds(String expression) {
        assertEquals("whitespace", formatter.format(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\fwhitespace", "whitespace\f", "\fwhitespace\f\f"})
    void removeFormFeeds(String expression) {
        assertEquals("whitespace", formatter.format(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\rwhitespace", "whitespace\r", "\rwhitespace\r\r"})
    void removeCarriageReturnFeeds(String expression) {
        assertEquals("whitespace", formatter.format(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {" whitespace", "whitespace ", " whitespace  "})
    void removeSeparatorSpaces(String expression) {
        assertEquals("whitespace", formatter.format(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {" \r\t\fwhitespace", "whitespace \n \r", "   \f\t whitespace  \r \n"})
    void removeMixedWhitespaceChars(String expression) {
        assertEquals("whitespace", formatter.format(expression));
    }
}
