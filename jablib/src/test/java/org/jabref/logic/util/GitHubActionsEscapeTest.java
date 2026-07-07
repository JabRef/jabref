package org.jabref.logic.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitHubActionsEscapeTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            # percent must be encoded
            100% done             | 100%25 done
            # newlines are encoded
            'first\nsecond'       | first%0Asecond
            # carriage return is encoded
            'first\rsecond'       | first%0Dsecond
            # colon and comma are left untouched in data position
            foo:bar,baz           | foo:bar,baz
            # nothing to escape
            hello world           | hello world
            """)
    void data(String input, String expected) {
        assertEquals(expected, GitHubActionsEscape.data(input));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            # Windows-style drive letter and backslash path
            C:\\foo\\bar.bib      | C%3A\\foo\\bar.bib
            # comma is encoded so it cannot terminate the property
            'a,b'                 | a%2Cb
            # percent is encoded before colon so the %3A insertion is not re-escaped to %253A
            '%:'                  | %25%3A
            # newlines still encode in property position
            'a\nb'                | a%0Ab
            """)
    void property(String input, String expected) {
        assertEquals(expected, GitHubActionsEscape.property(input));
    }
}
