package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class ToUpperCaseTest {

    @Test
    public void testEmpty() {
        assertEquals("", new ToUpperCase().format(""));
    }

    @Test
    public void testNull() {
        assertNull(new ToUpperCase().format(null));
    }

    @Test
    public void testLowerCase() {
        assertEquals("ABCD EFG", new ToUpperCase().format("abcd efg"));
    }

    @Test
    public void testUpperCase() {
        assertEquals("ABCD EFG", new ToUpperCase().format("ABCD EFG"));
    }

    @Test
    public void testMixedCase() {
        assertEquals("ABCD EFG", new ToUpperCase().format("abCD eFg"));
    }
}
