package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorFirstAbbrLastCommasTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
            # Empty case
            '', ''
            # Single Names
            'V. S. Someone', 'Someone, Van Something'
            # Two names
            'J. von Neumann and P. Black Brown', 'John von Neumann and Black Brown, Peter'
            # Three names
            'J. von Neumann, J. Smith and P. Black Brown', 'von Neumann, John and Smith, John and Black Brown, Peter'
            'J. von Neumann, J. Smith and P. Black Brown', 'John von Neumann and John Smith and Black Brown, Peter'
            """)
    void format(String expected, String input) {
        LayoutFormatter formatter = new AuthorFirstAbbrLastCommas();
        assertEquals(expected, formatter.format(input));
    }
}
