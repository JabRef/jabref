package net.sf.jabref.logic.formatter.bibtexfields;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;


public class UnicodeConverterTest {

    private final UnicodeToLatexFormatter conv = new UnicodeToLatexFormatter();

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testBasic() {
        assertEquals("aaa", conv.format("aaa"));
    }

    @Test
    public void testEmpty() {
        assertEquals("", conv.format(""));
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

    @Test(expected = NullPointerException.class)
    public void testUnicodeNull() {
        conv.format(null);
    }

    @Test
    public void testUnicodeEmpty() {
        assertEquals("", conv.format(""));
    }

}
