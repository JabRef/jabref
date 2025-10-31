package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorLastFirstAbbrOxfordCommasTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorLastFirstAbbrOxfordCommas#format(java.lang.String)}.
     */
    @ParameterizedTest
    @CsvSource({
            // Empty case
            "'', ''",

            // Single Names
            "Van Something Someone, 'Someone, V. S.'",

            // Two names
            "'John von Neumann and Black Brown, Peter', 'von Neumann, J. and Black Brown, P.'",

            // Three names
            "'von Neumann, John and Smith, John and Black Brown, Peter', 'von Neumann, J., Smith, J., and Black Brown, P.'",
            "'John von Neumann and John Smith and Black Brown, Peter', 'von Neumann, J., Smith, J., and Black Brown, P.'"
    })
    void format(String input, String expected) {
        LayoutFormatter formatter = new AuthorLastFirstAbbrOxfordCommas();
        assertEquals(expected, formatter.format(input));
    }
}
