package org.jabref.logic.formatter.bibtexfields;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class UnicodeConverterTest {

    private  UnicodeToLatexFormatter formatter;

    @Before
    public void setUp() {
        formatter = new UnicodeToLatexFormatter();
    }

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
