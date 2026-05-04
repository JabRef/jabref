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
    @ValueSource(strings = {
            // remove horizontal tabulation
            "\twhitespace",
            "whitespace\t",
            "\twhitespace\t\t",

            // remove line feeds
            "\nwhitespace",
            "whitespace\n",
            "\nwhitespace\n\n",

            // remove form feeds
            "\fwhitespace",
            "whitespace\f",
            "\fwhitespace\f\f",

            // remove carriage returns
            "\rwhitespace",
            "whitespace\r",
            "\rwhitespace\r\r",

            // remove spaces
            " whitespace",
            "whitespace ",
            " whitespace  ",

            // remove combinations
            " \r\t\fwhitespace",
            "whitespace \n \r",
            "   \f\t whitespace  \r \n",
    })
    void removeBlankCharacters(String expression) {
        assertEquals("whitespace", formatter.format(expression));
    }
}
