package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class UnicodeConverterTest {

    private UnicodeToLatexFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new UnicodeToLatexFormatter();
    }

    @Test
    void basic() {
        assertEquals("aaa", formatter.format("aaa"));
    }

    @Test
    void unicodeCombiningAccents() {
        assertEquals("{\\\"{a}}", formatter.format("a\u0308"));
        assertEquals("{\\\"{a}}b", formatter.format("a\u0308b"));
    }

    @Test
    void unicode() {
        assertEquals("{\\\"{a}}", formatter.format("ä"));
        assertEquals("{{$\\Epsilon$}}", formatter.format("\u0395"));
    }

    @Test
    void unicodeSingle() {
        assertEquals("a", formatter.format("a"));
    }
}
