package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorFirstLastOxfordCommasTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorFirstLastOxfordCommas#format(java.lang.String)}.
     */
    @ParameterizedTest
    @CsvSource(
            delimiterString = "->",
            textBlock = """
                        '' -> ''
                        'Van Something Someone' -> 'Someone, Van Something'
                        'John von Neumann and Peter Black Brown' -> 'John von Neumann and Peter Black Brown'
                        'John von Neumann, John Smith, and Peter Black Brown' -> 'von Neumann, John and Smith, John and Black Brown, Peter'
                        'John von Neumann, John Smith, and Peter Black Brown' -> 'John von Neumann and John Smith and Black Brown, Peter'
                    """
    )
    void format(String expected, String input) {
        LayoutFormatter formatter = new AuthorFirstLastOxfordCommas();
        assertEquals(expected, formatter.format(input));
    }
}
