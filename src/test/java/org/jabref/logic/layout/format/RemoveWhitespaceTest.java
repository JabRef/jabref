package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RemoveWhitespaceTest {

    @Test
    public void testEmptyExpectEmpty() {
        assertEquals("", new RemoveWhitespace().format(""));
    }

    @Test
    public void testNullExpectNull() {
        assertNull(new RemoveWhitespace().format(null));
    }

    @Test
    public void testNormal() {
        assertEquals("abcd EFG", new RemoveWhitespace().format("abcd EFG"));
    }

    @Test
    public void testTab() {
        assertEquals("abcd EFG", new RemoveWhitespace().format("abcd\t EFG"));
    }

    @Test
    public void testNewLineCombo() {
        assertEquals("abcd EFG", new RemoveWhitespace().format("abcd\r E\nFG\r\n"));
    }
}
