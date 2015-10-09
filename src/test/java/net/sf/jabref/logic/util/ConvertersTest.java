package net.sf.jabref.logic.util;

import net.sf.jabref.logic.util.strings.Converters;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConvertersTest {
    
    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }
    
    @Test
    public void testNumberOfModes() {
        Assert.assertEquals("Unicode to LaTeX", Converters.UNICODE_TO_LATEX.getName());
        Assert.assertEquals("HTML to LaTeX", Converters.HTML_TO_LATEX.getName());
    }

    @Test
    public void testUnicodeToLatexConversion() {
        Assert.assertEquals("", Converters.UNICODE_TO_LATEX.convert(""));
        Assert.assertEquals("abc", Converters.UNICODE_TO_LATEX.convert("abc"));
        Assert.assertEquals("{{\\aa}}{\\\"{a}}{\\\"{o}}", Converters.UNICODE_TO_LATEX.convert("\u00E5\u00E4\u00F6"));
    }

    @Test
    public void testHTMLToLatexConversion() {
        Assert.assertEquals("", Converters.HTML_TO_LATEX.convert(""));
        Assert.assertEquals("abc", Converters.HTML_TO_LATEX.convert("abc"));
        Assert.assertEquals("{{\\aa}}{\\\"{a}}{\\\"{o}}", Converters.HTML_TO_LATEX.convert("&aring;&auml;&ouml;"));
        Assert.assertEquals("{\\'{\\i}}", Converters.HTML_TO_LATEX.convert("i&#x301;"));
    }

}
