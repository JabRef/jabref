package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ToLowerCaseTest {

    @Test
    public void testEmpty() {
        assertEquals("", new ToLowerCase().format(""));
    }

    @Test
    public void testNull() {
        assertNull(new ToLowerCase().format(null));
    }

    @Test
    public void testLowerCase() {
        assertEquals("abcd efg ", new ToLowerCase().format("abcd efg "));
    }

    @Test
    public void testUpperCase() {
        assertEquals("abcd efg", new ToLowerCase().format("ABCD EFG"));
    }

    @Test
    public void testMixedCase() {
        assertEquals("abcd efg", new ToLowerCase().format("abCD eFg"));
    }

    @Test
    public void includeNumbersInString() {
        assertEquals("abcd123efg", new ToLowerCase().format("abCD123eFg"));
    }

    @Test
    public void includeSpecialCharactersInString() {
        assertEquals("hello!*#", new ToLowerCase().format("Hello!*#"));
    }

    @Test
    public void includeOnlyNumbersAndSpecialCharacters() {
        assertEquals("123*%&456", new ToLowerCase().format("123*%&456"));
    }
}
