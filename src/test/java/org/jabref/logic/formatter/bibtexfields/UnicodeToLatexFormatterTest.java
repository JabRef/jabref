package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnicodeToLatexFormatterTest {

    private UnicodeToLatexFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new UnicodeToLatexFormatter();
    }

    @Test
    void formatWithoutUnicodeCharactersReturnsSameString() {
        assertEquals("abc", formatter.format("abc"));
    }

    @Test
    void formatMultipleUnicodeCharacters() {
        assertEquals("{{\\aa}}{\\\"{a}}{\\\"{o}}", formatter.format("\u00E5\u00E4\u00F6"));
    }

    @Test
    void formatHighCodepointUnicodeCharacter() {
        assertEquals("$\\epsilon$", formatter.format("\uD835\uDF16"));
    }

    @Test
    void formatExample() {
        assertEquals("M{\\\"{o}}nch", formatter.format(formatter.getExampleInput()));
    }
}
