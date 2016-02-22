package net.sf.jabref.logic.formatter.bibtexfields;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.formatter.bibtexfields.HTMLToLatexFormatter;


public class HTMLConverterTest {

    HTMLToLatexFormatter conv = new HTMLToLatexFormatter();

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testBasic() {
        assertEquals("aaa", conv.format("aaa"));
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
}
