package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrdinalTest {

    @Test
    void empty() {
        assertEquals("", new Ordinal().format(""));
    }

    @Test
    void testNull() {
        assertNull(new Ordinal().format(null));
    }

    @Test
    void singleDigit() {
        assertEquals("1st", new Ordinal().format("1"));
        assertEquals("2nd", new Ordinal().format("2"));
        assertEquals("3rd", new Ordinal().format("3"));
        assertEquals("4th", new Ordinal().format("4"));
    }

    @Test
    void multiDigits() {
        assertEquals("11th", new Ordinal().format("11"));
        assertEquals("111th", new Ordinal().format("111"));
        assertEquals("21st", new Ordinal().format("21"));
    }

    @Test
    void alreadyOrdinals() {
        assertEquals("1st", new Ordinal().format("1st"));
        assertEquals("111th", new Ordinal().format("111th"));
        assertEquals("22nd", new Ordinal().format("22nd"));
    }

    @Test
    void fullSentence() {
        assertEquals("1st edn.", new Ordinal().format("1 edn."));
        assertEquals("1st edition", new Ordinal().format("1st edition"));
        assertEquals("The 2nd conference on 3rd.14th", new Ordinal().format("The 2 conference on 3.14"));
    }

    @Test
    void letters() {
        assertEquals("abCD eFg", new Ordinal().format("abCD eFg"));
    }
}
