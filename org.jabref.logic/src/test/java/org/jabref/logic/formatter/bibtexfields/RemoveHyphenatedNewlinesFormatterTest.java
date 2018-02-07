package org.jabref.logic.formatter.bibtexfields;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoveHyphenatedNewlinesFormatterTest {
    private RemoveHyphenatedNewlinesFormatter formatter;

    @Before
    public void setUp() {
        formatter = new RemoveHyphenatedNewlinesFormatter();
    }

    @Test
    public void removeHyphensBeforeNewlines() {
        assertEquals("water", formatter.format("wa-\nter"));
        assertEquals("water", formatter.format("wa-\r\nter"));
        assertEquals("water", formatter.format("wa-\rter"));
    }
}
