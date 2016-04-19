package net.sf.jabref.importer.fileformat;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FieldContentParserTest {

    private FieldContentParser parser;

    @BeforeClass
    public static void loadPreferences() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        parser = new FieldContentParser();
    }

    @Test
    public void unifiesLineBreaks() {
        String original = "I\r\nunify\nline\rbreaks.";
        String expected = "I\nunify\nline\nbreaks.".replace("\n", Globals.NEWLINE);
        String processed = parser.format(new StringBuilder(original), "abstract").toString();

        assertEquals(expected, processed);
    }

    @Test
    public void retainsWhitespaceForMultiLineFields() {
        String original = "I\nkeep\nline\nbreaks\nand\n\ttabs.";
        String formatted = original.replace("\n", Globals.NEWLINE);

        String abstrakt = parser.format(new StringBuilder(original), "abstract").toString();
        String review = parser.format(new StringBuilder(original), "review").toString();

        assertEquals(formatted, abstrakt);
        assertEquals(formatted, review);
    }

    @Test
    public void removeWhitespaceFromNonMultiLineFields() {
        String original = "I\nshould\nnot\ninclude\nadditional\nwhitespaces  \nor\n\ttabs.";
        String expected = "I should not include additional whitespaces or tabs.";

        String abstrakt = parser.format(new StringBuilder(original), "title").toString();
        String any = parser.format(new StringBuilder(original), "anyotherfield").toString();

        assertEquals(expected, abstrakt);
        assertEquals(expected, any);
    }
}