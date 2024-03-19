package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class UnicodeConverterTest {

    private UnicodeToLatexFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new UnicodeToLatexFormatter();
    }

    @Test
    public void basic() {
        assertEquals("aaa", formatter.format("aaa"));
    }

    @Test
    public void unicodeCombiningAccents() {
        assertEquals("{\\\"{a}}", formatter.format("a\u0308"));
        assertEquals("{\\\"{a}}b", formatter.format("a\u0308b"));
    }

    @Test
    public void unicode() {
        assertEquals("{\\\"{a}}", formatter.format("ä"));
        assertEquals("{{$\\Epsilon$}}", formatter.format("\u0395"));
    }

    @Test
    public void unicodeSingle() {
        assertEquals("a", formatter.format("a"));
    }
}
