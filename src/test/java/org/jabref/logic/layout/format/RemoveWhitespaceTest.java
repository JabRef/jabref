package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RemoveWhitespaceTest {

    @Test
    void emptyExpectEmpty() {
        assertEquals("", new RemoveWhitespace().format(""));
    }

    @Test
    void nullExpectNull() {
        assertNull(new RemoveWhitespace().format(null));
    }

    @Test
    void normal() {
        assertEquals("abcd EFG", new RemoveWhitespace().format("abcd EFG"));
    }

    @Test
    void tab() {
        assertEquals("abcd EFG", new RemoveWhitespace().format("abcd\t EFG"));
    }

    @Test
    void newLineCombo() {
        assertEquals("abcd EFG", new RemoveWhitespace().format("abcd\r E\nFG\r\n"));
    }
}
