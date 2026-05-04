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
    @CsvSource(
            delimiterString = "->",
            textBlock = """
                        some text -> '{some text}'
                        some text -> '{some text'
                        some text -> 'some text}'
                        '\\some text\\' -> '\\{some text\\}'
                        some text -> some text
                    """
    )
    void format(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
