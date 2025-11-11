package org.jabref.logic.layout.format;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case that verifies the functionalities of the formatter AuthorLastFirstAbbreviator.
 */
class AuthorLastFirstAbbreviatorTest {

    private final AuthorLastFirstAbbreviator abbreviator = new AuthorLastFirstAbbreviator();

    @ParameterizedTest
    @CsvSource(
            delimiterString = "->",
            textBlock = """
                        'Lastname, N.' -> 'Lastname, Name'
                        'Lastname, N. M.' -> 'Lastname, Name Middlename'
                        'Lastname, N. M. and Sobrenome, N. N.' -> 'Lastname, Name Middlename and Sobrenome, Nome Nomedomeio'
                        'Other, Jr., A. N.' -> 'Other, Jr., Anthony N.'
                        '' -> ''
                        'Someone, V. S.' -> 'Someone, Van Something'
                        'Smith, J.' -> 'Smith, John'
                        'von Neumann, J. and Smith, J. and Black Brown, P.' -> 'von Neumann, John and Smith, John and Black Brown, Peter'
                    """
    )
    void abbreviate(String expected, String input) {
        assertEquals(expected, abbreviator.format(input));
    }
}
