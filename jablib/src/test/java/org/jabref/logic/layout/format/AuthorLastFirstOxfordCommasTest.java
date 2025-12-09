package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorLastFirstOxfordCommasTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorLastFirstOxfordCommas#format(java.lang.String)}.
     */
    @ParameterizedTest
    @CsvSource(textBlock = """
            # Empty case
            '' , ''
            # Single Names
            'Someone, Van Something' , 'Van Something Someone'
            # Two names
            'von Neumann, John and Black Brown, Peter' , 'John von Neumann and Black Brown, Peter'
            # Three names
            'von Neumann, John, Smith, John, and Black Brown, Peter' , 'von Neumann, John and Smith, John and Black Brown, Peter'
            'von Neumann, John, Smith, John, and Black Brown, Peter' , 'John von Neumann and John Smith and Black Brown, Peter'
            """)
    void format_cases(String expected, String input) {
        LayoutFormatter formatter = new AuthorLastFirstOxfordCommas();
        assertEquals(expected, formatter.format(input));
    }
}
