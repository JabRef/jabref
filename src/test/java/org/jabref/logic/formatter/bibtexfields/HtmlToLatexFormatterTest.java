package org.jabref.logic.formatter.bibtexfields;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class HtmlToLatexFormatterTest {

    private HtmlToLatexFormatter formatter;

    @Before
    public void setUp() {
        formatter = new HtmlToLatexFormatter();
    }

    @Test
    public void formatWithoutHtmlCharactersReturnsSameString() {
        assertEquals("abc", formatter.format("abc"));
    }

    @Test
    public void formatMultipleHtmlCharacters() {
        assertEquals("{{\\aa}}{\\\"{a}}{\\\"{o}}", formatter.format("&aring;&auml;&ouml;"));
    }

    @Test
    public void formatCombinedAccent() {
        assertEquals("{\\'{\\i}}", formatter.format("i&#x301;"));
    }

    @Test
    public void testBasic() {
        assertEquals("aaa", formatter.format("aaa"));
    }

    @Test
    public void testHTML() {
        assertEquals("{\\\"{a}}", formatter.format("&auml;"));
        assertEquals("{\\\"{a}}", formatter.format("&#228;"));
        assertEquals("{\\\"{a}}", formatter.format("&#xe4;"));
        assertEquals("{{$\\Epsilon$}}", formatter.format("&Epsilon;"));
    }

    @Test
    public void testHTMLRemoveTags() {
        assertEquals("aaa", formatter.format("<b>aaa</b>"));
    }

    @Test
    public void testHTMLCombiningAccents() {
        assertEquals("{\\\"{a}}", formatter.format("a&#776;"));
        assertEquals("{\\\"{a}}", formatter.format("a&#x308;"));
        assertEquals("{\\\"{a}}b", formatter.format("a&#776;b"));
        assertEquals("{\\\"{a}}b", formatter.format("a&#x308;b"));
    }

    @Test
    public void formatExample() {
        assertEquals("JabRef", formatter.format(formatter.getExampleInput()));
    }
}
