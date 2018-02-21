package org.jabref.logic.formatter.bibtexfields;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoveNewlinesFormatterTest {
    private RemoveNewlinesFormatter formatter;

    @Before
    public void setUp() {
        formatter = new RemoveNewlinesFormatter();
    }

    @Test
    public void removeCarriageReturnLineFeed() {
        assertEquals("rn linebreak", formatter.format("rn\r\nlinebreak"));
    }

    @Test
    public void removeCarriageReturn() {
        assertEquals("r linebreak", formatter.format("r\rlinebreak"));
    }

    @Test
    public void removeLineFeed() {
        assertEquals("n linebreak", formatter.format("n\nlinebreak"));
    }
}
