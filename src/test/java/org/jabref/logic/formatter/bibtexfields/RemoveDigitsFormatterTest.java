package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveDigitsFormatterTest {

    private RemoveDigitsFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveDigitsFormatter();
    }

    @Test
    public void doNothingIfSingleSpace() {
        assertEquals("one digit", formatter.format("one 1 digit"));
    }

    @Test
    public void doNothingIfNoSpace() {
        assertEquals("two digits", formatter.format("two 01 digits"));
    }

    @Test
    public void removeAllButOneSpacesIfTwo() {
        assertEquals("no digits", formatter.format("no digits"));
    }
}
