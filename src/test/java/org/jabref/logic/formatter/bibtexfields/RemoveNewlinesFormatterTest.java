
package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveNewlinesFormatterTest {

    private RemoveNewlinesFormatter formatter;

    @BeforeEach
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

    @Test
    public void withoutNewLineUnmodified() {
        assertEquals("no linebreak", formatter.format("no linebreak"));
    }

    @Test
    public void removePlatformSpecificNewLine() {
        String newLine = String.format("%n");
        assertEquals("linebreak on current platform", formatter.format("linebreak on" + newLine + "current platform"));
    }
}
