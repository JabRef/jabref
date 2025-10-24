package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorAndsReplacerTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorAndsReplacer#format(java.lang.String)}.
     */
    @ParameterizedTest
    @CsvSource({
            // Empty case
            "'', ''",

            // Single name (unchanged)
            "'Someone, Van Something', 'Someone, Van Something'",

            // 'and' replaced with '&' in two names
            "'John Smith and Black Brown, Peter', 'John Smith & Black Brown, Peter'",

            // 'and' replaced with ';' and '&' for three names
            "'von Neumann, John and Smith, John and Black Brown, Peter', 'von Neumann, John; Smith, John & Black Brown, Peter'",

            // 'and' replaced with ';' and '&' for three names
            "'John von Neumann and John Smith and Peter Black Brown', 'John von Neumann; John Smith & Peter Black Brown'"
    })
    void format(String input, String expected) {
        LayoutFormatter a = new AuthorAndsReplacer();
        assertEquals(expected, a.format(input));
    }
}
