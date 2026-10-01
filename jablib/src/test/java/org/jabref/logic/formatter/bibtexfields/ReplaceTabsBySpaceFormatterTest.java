package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplaceTabsBySpaceFormatterTest {

    private ReplaceTabsBySpaceFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ReplaceTabsBySpaceFormatter();
    }

    @Test
    void removeSingleTab() {
        assertEquals("single tab", formatter.format("single\ttab"));
    }

    @Test
    void removeMultipleTabs() {
        assertEquals("multiple tabs", formatter.format("multiple\t\ttabs"));
    }

    @Test
    void doNothingIfNoTab() {
        assertEquals("notab", formatter.format("notab"));
    }
}
