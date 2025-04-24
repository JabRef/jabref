package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveHyphenatedNewlinesFormatterTest {

    private RemoveHyphenatedNewlinesFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveHyphenatedNewlinesFormatter();
    }

    @Test
    void removeHyphensBeforeNewlines() {
        assertEquals("water", formatter.format("wa-\nter"));
        assertEquals("water", formatter.format("wa-\r\nter"));
        assertEquals("water", formatter.format("wa-\rter"));
    }

    @Test
    void withoutHyphensUnmodified() {
        assertEquals("water", formatter.format("water"));
    }

    @Test
    void removeHyphensBeforePlatformSpecificNewlines() {
        String newLine = "%n".formatted();
        assertEquals("water", formatter.format("wa-" + newLine + "ter"));
    }
}
