package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveBracketsTest {
    private LayoutFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveBrackets();
    }

    @ParameterizedTest
    @CsvSource({
            // Brace pair correctly removed
            "{some text}, some text",

            // Single opening brace correctly removed
            "{some text, some text",

            // Single closing brace correctly removed
            "some text}, some text",

            // Brace pair with escaped backslash correctly removed
            "'\\\\{some text\\\\}', '\\\\some text\\\\'",

            // Without brackets unmodified
            "some text, some text"
    })
    void format(String input, String expected) {
        assertEquals(expected, formatter.format(input));
    }
}
