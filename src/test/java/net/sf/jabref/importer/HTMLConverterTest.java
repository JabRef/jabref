package net.sf.jabref.importer;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;


public class HTMLConverterTest {

    HTMLConverter conv = new HTMLConverter();

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testBasic() {
        assertEquals("aaa", conv.format("aaa"));
        assertEquals("aaa", conv.formatUnicode("aaa"));
    }

    @Test
    public void testHTMLNull() {
        assertEquals(null, conv.format(null));
    }

    @Test
    public void testHTMLEmpty() {
        assertEquals("", conv.format(""));
    }

    @Test
    public void testHTML() {
        assertEquals("{\\\"{a}}", conv.format("&auml;"));
        assertEquals("{\\\"{a}}", conv.format("&#228;"));
        assertEquals("{\\\"{a}}", conv.format("&#xe4;"));
        assertEquals("{$\\Epsilon$}", conv.format("&Epsilon;"));
    }

    @Test
    public void testHTMLRemoveTags() {
        assertEquals("aaa", conv.format("<b>aaa</b>"));
    }

    @Test
    public void testHTMLCombiningAccents() {
        assertEquals("{\\\"{a}}", conv.format("a&#776;"));
        assertEquals("{\\\"{a}}", conv.format("a&#x308;"));
        assertEquals("{\\\"{a}}b", conv.format("a&#776;b"));
        assertEquals("{\\\"{a}}b", conv.format("a&#x308;b"));
    }

    @Test
    public void testUnicodeCombiningAccents() {
        assertEquals("{\\\"{a}}", conv.formatUnicode("a\u0308"));
        assertEquals("{\\\"{a}}b", conv.formatUnicode("a\u0308b"));
    }

    @Test
    public void testUnicode() {
        assertEquals("{\\\"{a}}", conv.formatUnicode("ä"));
        assertEquals("{$\\Epsilon$}", conv.formatUnicode("\u0395"));
    }

    @Test
    public void testUnicodeSingle() {
        assertEquals("a", conv.formatUnicode("a"));
    }

    @Test
    public void testUnicodeNull() {
        assertEquals(null, conv.formatUnicode(null));
    }

    @Test
    public void testUnicodeEmpty() {
        assertEquals("", conv.formatUnicode(""));
    }

}
