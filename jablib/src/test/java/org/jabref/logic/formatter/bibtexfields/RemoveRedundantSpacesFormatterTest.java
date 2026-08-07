package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveRedundantSpacesFormatterTest {

    private RemoveRedundantSpacesFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveRedundantSpacesFormatter();
    }

    @Test
    void doNothingIfSingleSpace() {
        assertEquals("single space", formatter.format("single space"));
    }

    @Test
    void doNothingIfNoSpace() {
        assertEquals("nospace", formatter.format("nospace"));
    }

    @Test
    void removeAllButOneSpacesIfTwo() {
        assertEquals("two spaces", formatter.format("two  spaces"));
    }

    @Test
    void removeAllButOneSpacesIfThree() {
        assertEquals("three spaces", formatter.format("three   spaces"));
    }
}
