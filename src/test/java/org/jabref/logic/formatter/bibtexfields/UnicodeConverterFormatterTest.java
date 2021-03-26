package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class UnicodeConverterFormatterTest {

    private static final UnicodeToLatexFormatter formatter = new UnicodeToLatexFormatter();

    @Test
    public void testBasic() {
        assertEquals("aaa", formatter.format("aaa"));
    }

    @Test
    public void testUnicodeCombiningAccents() {
        assertEquals("{\\\"{a}}", formatter.format("a\u0308"));
        assertEquals("{\\\"{a}}b", formatter.format("a\u0308b"));
    }

    @Test
    public void testUnicode() {
        assertEquals("{\\\"{a}}", formatter.format("ä"));
        assertEquals("{{$\\Epsilon$}}", formatter.format("\u0395"));
    }

    @Test
    public void testUnicodeSingle() {
        assertEquals("a", formatter.format("a"));
    }
}
