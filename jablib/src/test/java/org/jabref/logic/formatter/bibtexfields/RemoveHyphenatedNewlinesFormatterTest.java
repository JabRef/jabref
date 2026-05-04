package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveHyphenatedNewlinesFormatterTest {

    private RemoveHyphenatedNewlinesFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveHyphenatedNewlinesFormatter();
    }

    @ParameterizedTest
    @ValueSource(strings = {"wa-\nter", "wa-\r\nter", "wa-\rter"})
    void removeHyphensBeforeNewlines(String expression) {
        assertEquals("water", formatter.format(expression));
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
