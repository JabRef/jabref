
package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveNewlinesFormatterTest {

    private RemoveNewlinesFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveNewlinesFormatter();
    }

    @Test
    void removeCarriageReturnLineFeed() {
        assertEquals("rn linebreak", formatter.format("rn\r\nlinebreak"));
    }

    @Test
    void removeCarriageReturn() {
        assertEquals("r linebreak", formatter.format("r\rlinebreak"));
    }

    @Test
    void removeLineFeed() {
        assertEquals("n linebreak", formatter.format("n\nlinebreak"));
    }

    @Test
    void withoutNewLineUnmodified() {
        assertEquals("no linebreak", formatter.format("no linebreak"));
    }

    @Test
    void removePlatformSpecificNewLine() {
        String newLine = "%n".formatted();
        assertEquals("linebreak on current platform", formatter.format("linebreak on" + newLine + "current platform"));
    }
}
