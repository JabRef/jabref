package org.jabref.logic.layout.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveLatexCommandsFormatterTest {

    private RemoveLatexCommandsFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveLatexCommandsFormatter();
    }

    @ParameterizedTest
    @CsvSource({
            // Without LaTeX commands
            "some text, some text",

            // Single command wiped
            "\\sometext, ''",

            // Single space after command removed
            "\\some text, text",

            // Multiple spaces after command removed
            "\\some     text, text",

            // Escaped backslash becomes backslash
            "'\\\\', '\\'",

            // Escaped backslash followed by text
            "'\\\\some text', '\\some text'",

            // Escaped backslash kept
            "'\\\\some text\\\\', '\\some text\\'",

            // Escaped underscore replaced
            "some\\_text, some_text",

            // Realistic LaTeX URL with escaped underscores
            "'http://pi.informatik.uni-siegen.de/stt/36\\_2/./03\\_Technische\\_Beitraege/ZEUS2016/beitrag\\_2.pdf', 'http://pi.informatik.uni-siegen.de/stt/36_2/./03_Technische_Beitraege/ZEUS2016/beitrag_2.pdf'"
    })
    void format(String input, String expected) {
        assertEquals(expected, formatter.format(input));
    }
}
