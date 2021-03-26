package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveHyphenatedNewlinesFormatterTest {

    private static final RemoveHyphenatedNewlinesFormatter formatter = new RemoveHyphenatedNewlinesFormatter();

    @Test
    public void removeHyphensBeforeNewlines() {
        assertEquals("water", formatter.format("wa-\nter"));
        assertEquals("water", formatter.format("wa-\r\nter"));
        assertEquals("water", formatter.format("wa-\rter"));
    }

    @Test
    public void removeHyphensBeforePlatformSpecificNewlines() {
        String newLine = String.format("%n");
        assertEquals("water", formatter.format("wa-" + newLine + "ter"));
    }
}
