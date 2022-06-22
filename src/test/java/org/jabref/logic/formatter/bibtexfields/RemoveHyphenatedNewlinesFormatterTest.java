package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveHyphenatedNewlinesFormatterTest {

    private RemoveHyphenatedNewlinesFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveHyphenatedNewlinesFormatter();
    }

    @Test
    public void removeHyphensBeforeNewlines() {
        assertEquals("water", formatter.format("wa-\nter"));
        assertEquals("water", formatter.format("wa-\r\nter"));
        assertEquals("water", formatter.format("wa-\rter"));
    }

    @Test
    public void withoutHyphensUnmodified() {
        assertEquals("water", formatter.format("water"));
    }

    @Test
    public void removeHyphensBeforePlatformSpecificNewlines() {
        String newLine = String.format("%n");
        assertEquals("water", formatter.format("wa-" + newLine + "ter"));
    }
}
