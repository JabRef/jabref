package net.sf.jabref.logic.formatter.bibtexfields;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;


/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class UnicodeConverterTest {

    private final UnicodeToLatexFormatter conv = new UnicodeToLatexFormatter();

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testBasic() {
        assertEquals("aaa", conv.format("aaa"));
    }

    @Test
    public void testUnicodeCombiningAccents() {
        assertEquals("{\\\"{a}}", conv.format("a\u0308"));
        assertEquals("{\\\"{a}}b", conv.format("a\u0308b"));
    }

    @Test
    public void testUnicode() {
        assertEquals("{\\\"{a}}", conv.format("ä"));
        assertEquals("{$\\Epsilon$}", conv.format("\u0395"));
    }

    @Test
    public void testUnicodeSingle() {
        assertEquals("a", conv.format("a"));
    }

}
