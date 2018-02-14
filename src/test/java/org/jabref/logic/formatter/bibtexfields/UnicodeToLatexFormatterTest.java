package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class UnicodeToLatexFormatterTest {

    private UnicodeToLatexFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new UnicodeToLatexFormatter();
    }

    @Test
    public void formatWithoutUnicodeCharactersReturnsSameString() {
        assertEquals("abc", formatter.format("abc"));
    }

    @Test
    public void formatMultipleUnicodeCharacters() {
        assertEquals("{{\\aa}}{\\\"{a}}{\\\"{o}}", formatter.format("\u00E5\u00E4\u00F6"));
    }

    @Test
    public void formatExample() {
        assertEquals("M{\\\"{o}}nch", formatter.format(formatter.getExampleInput()));
    }
}
