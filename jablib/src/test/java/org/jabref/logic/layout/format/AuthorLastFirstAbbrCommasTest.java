package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorLastFirstAbbrCommasTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorLastFirstAbbrCommas#format(java.lang.String)}.
     */
    @ParameterizedTest
    @CsvSource(textBlock = """
            # Empty case
            '', ''
            # Single Names
            'Someone, V. S.', 'Van Something Someone'
            # Two Names
            'von Neumann, J. and Black Brown, P.', 'John von Neumann and Black Brown, Peter'
            # Three Names
            'von Neumann, J., Smith, J. and Black Brown, P.', 'von Neumann, John and Smith, John and Black Brown, Peter'
            'von Neumann, J., Smith, J. and Black Brown, P.', 'John von Neumann and John Smith and Black Brown, Peter'
            """)
    void format(String expected, String input) {
        LayoutFormatter formatter = new AuthorLastFirstAbbrCommas();
        assertEquals(expected, formatter.format(input));
    }
}
