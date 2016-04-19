package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class UnicodeToLatexFormatterTest {

    private final UnicodeToLatexFormatter formatter = new UnicodeToLatexFormatter();

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