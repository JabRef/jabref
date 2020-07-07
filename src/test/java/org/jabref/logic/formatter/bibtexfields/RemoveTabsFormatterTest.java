
package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveTabsFormatterTest {

    private RemoveTabsFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveTabsFormatter();
    }

    @Test
    public void removeSingleTab() {
        assertEquals("single tab", formatter.format("single\ttab"));
    }

    @Test
    public void removeMultipleTabs() {
        assertEquals("multiple tabs", formatter.format("multiple\t\ttabs"));
    }

    @Test
    public void doNothingIfNoTab() {
        assertEquals("notab", formatter.format("notab"));
    }
}
