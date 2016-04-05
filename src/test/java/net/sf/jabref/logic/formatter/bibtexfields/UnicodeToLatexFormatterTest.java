package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Test;

import static org.junit.Assert.*;

public class UnicodeToLatexFormatterTest {

    @Test
    public void formatWithoutUnicodeCharactersReturnsSameString() {
        assertEquals("abc", new UnicodeToLatexFormatter().format("abc"));
    }

    @Test
    public void formatMultipleUnicodeCharacters() {
        assertEquals("{{\\aa}}{\\\"{a}}{\\\"{o}}", new UnicodeToLatexFormatter().format("\u00E5\u00E4\u00F6"));
    }
}