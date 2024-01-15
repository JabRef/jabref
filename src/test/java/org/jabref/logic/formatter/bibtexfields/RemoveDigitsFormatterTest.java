package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveDigitsFormatterTest {

    private RemoveDigitsFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveDigitsFormatter();
    }

    @Test
    void doNothingIfSingleSpace() {
        assertEquals("one digit", formatter.format("one 1 digit"));
    }

    @Test
    void doNothingIfNoSpace() {
        assertEquals("two digits", formatter.format("two 01 digits"));
    }

    @Test
    void removeAllButOneSpacesIfTwo() {
        assertEquals("no digits", formatter.format("no digits"));
    }
}
