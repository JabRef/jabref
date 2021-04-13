package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveRedundantSpacesFormatterTest {

    private RemoveRedundantSpacesFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveRedundantSpacesFormatter();
    }

    @Test
    public void doNothingIfSingleSpace() {
        assertEquals("single space", formatter.format("single space"));
    }

    @Test
    public void doNothingIfNoSpace() {
        assertEquals("nospace", formatter.format("nospace"));
    }

    @Test
    public void removeAllButOneSpacesIfTwo() {
        assertEquals("two spaces", formatter.format("two  spaces"));
    }

    @Test
    public void removeAllButOneSpacesIfThree() {
        assertEquals("three spaces", formatter.format("three   spaces"));
    }
}
