package org.jabref.logic.layout.format;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case  that verifies the functionalities of the formater AuthorLastFirstAbbreviator.
 */
class AuthorLastFirstAbbreviatorTest {

    private final AuthorLastFirstAbbreviator abbreviator = new AuthorLastFirstAbbreviator();

    @ParameterizedTest
    @CsvSource({
            // One author, simple name
            "'Lastname, Name', 'Lastname, N.'",

            // One author, common name with middle name
            "'Lastname, Name Middlename', 'Lastname, N. M.'",

            // Two authors with common names
            "'Lastname, Name Middlename and Sobrenome, Nome Nomedomeio', 'Lastname, N. M. and Sobrenome, N. N.'",

            // Author with Jr. suffix
            "'Other, Jr., Anthony N.', 'Other, Jr., A. N.'",

            // Empty input returns empty
            "'', ''",

            // Author with prefix like Van
            "'Someone, Van Something', 'Someone, V. S.'",

            // Single author
            "'Smith, John', 'Smith, J.'",

            // Multiple authors with complex last names
            "'von Neumann, John and Smith, John and Black Brown, Peter', 'von Neumann, J. and Smith, J. and Black Brown, P.'"
    })
    void abbreviate(String input, String expected) {
        assertEquals(expected, abbreviator.format(input));
    }
}
