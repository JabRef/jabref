package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NonSpaceWhitespaceRemoverTest {
    private final NonSpaceWhitespaceRemover nonSpaceWhitespaceRemover = new NonSpaceWhitespaceRemover();

    @ParameterizedTest
    @CsvSource({
            "'', ''",
            "'abcd EFG', 'abcd EFG'",
            "'abcd    EFG', 'abcd    EFG'",
            "'abcd\t EFG', 'abcd EFG'",
            "'abcd\r E\nFG\r\n', 'abcd EFG'"
    })
    void format(String input, String expected) {
        assertEquals(expected, nonSpaceWhitespaceRemover.format(input));
    }

    @Test
    void nullInput() {
        assertNull(nonSpaceWhitespaceRemover.format(null));
    }
}
