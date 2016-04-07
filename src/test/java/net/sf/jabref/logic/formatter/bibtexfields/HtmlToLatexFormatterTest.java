package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HtmlToLatexFormatterTest {

    private HtmlToLatexFormatter htmlToLatexFormatter;

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        htmlToLatexFormatter = new HtmlToLatexFormatter();
    }

    @Test
    public void formatWithoutHtmlCharactersReturnsSameString() {
        assertEquals("abc", htmlToLatexFormatter.format("abc"));
    }

    @Test
    public void formatMultipleHtmlCharacters() {
        assertEquals("{{\\aa}}{\\\"{a}}{\\\"{o}}", htmlToLatexFormatter.format("&aring;&auml;&ouml;"));
    }

    @Test
    public void formatCombinedAccent() {
        assertEquals("{\\'{\\i}}", htmlToLatexFormatter.format("i&#x301;"));
    }

    @Test
    public void testBasic() {
        assertEquals("aaa", htmlToLatexFormatter.format("aaa"));
    }

    @Test
    public void testHTMLEmpty() {
        assertEquals("", htmlToLatexFormatter.format(""));
    }

    @Test
    public void testHTML() {
        assertEquals("{\\\"{a}}", htmlToLatexFormatter.format("&auml;"));
        assertEquals("{\\\"{a}}", htmlToLatexFormatter.format("&#228;"));
        assertEquals("{\\\"{a}}", htmlToLatexFormatter.format("&#xe4;"));
        assertEquals("{$\\Epsilon$}", htmlToLatexFormatter.format("&Epsilon;"));
    }

    @Test
    public void testHTMLRemoveTags() {
        assertEquals("aaa", htmlToLatexFormatter.format("<b>aaa</b>"));
    }

    @Test
    public void testHTMLCombiningAccents() {
        assertEquals("{\\\"{a}}", htmlToLatexFormatter.format("a&#776;"));
        assertEquals("{\\\"{a}}", htmlToLatexFormatter.format("a&#x308;"));
        assertEquals("{\\\"{a}}b", htmlToLatexFormatter.format("a&#776;b"));
        assertEquals("{\\\"{a}}b", htmlToLatexFormatter.format("a&#x308;b"));
    }
}