package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HtmlToUnicodeFormatterTest {

    private HtmlToUnicodeFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new HtmlToUnicodeFormatter();
    }

    @Test
    public void formatWithoutHtmlCharactersReturnsSameString() {
        assertEquals("abc", formatter.format("abc"));
    }

    @Test
    public void formatMultipleHtmlCharacters() {
        assertEquals("åäö", formatter.format("&aring;&auml;&ouml;"));
    }

    @Test
    public void formatCombinedAccent() {
        assertEquals("í", formatter.format("i&#x301;"));
    }

    @Test
    public void testBasic() {
        assertEquals("aaa", formatter.format("aaa"));
    }

    @Test
    public void testUmlauts() {
        assertEquals("ä", formatter.format("&auml;"));
        assertEquals("ä", formatter.format("&#228;"));
        assertEquals("ä", formatter.format("&#xe4;"));

    }

    @Test
    public void testGreekLetter() {
        assertEquals("Ε", formatter.format("&Epsilon;"));
    }

    @Test
    public void testHTMLRemoveTags() {
        assertEquals("aaa", formatter.format("<p>aaa</p>"));
    }

    @Test
    public void formatExample() {
        assertEquals("bread & butter", formatter.format(formatter.getExampleInput()));
    }
}


