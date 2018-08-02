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
        String newLine = String.format("%n");
        assertEquals("water", formatter.format("wa-" + newLine + "ter"));
    }
}
