package net.sf.jabref.logic.bibtex;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LatexFieldFormatterTests {

    private LatexFieldFormatter formatter;

    @BeforeClass
    public static void setUpBeforeClass(){
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Before
    public void setUp() {
        this.formatter = new LatexFieldFormatter(LatexFieldFormatterPreferences.fromPreferences(Globals.prefs));
    }

    @Test
    public void normalizeNewlineInAbstractField() {
        String fieldName = "abstract";
        String text = "lorem" + StringUtil.NEWLINE + " ipsum lorem ipsum\nlorem ipsum \rlorem ipsum\r\ntest";

        // The newlines are normalized according to the globally configured newline setting in the formatter
        String expected = "{" + "lorem" + StringUtil.NEWLINE + " ipsum lorem ipsum" + StringUtil.NEWLINE
 + "lorem ipsum "
                + StringUtil.NEWLINE + "lorem ipsum"
                + StringUtil.NEWLINE + "test" + "}";

        String result = formatter.format(text, fieldName);

        assertEquals(expected, result);
    }

    @Test
    public void preserveNewlineInAbstractField() {
        String fieldName = "abstract";
        // The newlines are normalized according to the globally configured newline setting in the formatter
        String text = "lorem ipsum lorem ipsum" + StringUtil.NEWLINE + "lorem ipsum lorem ipsum" + StringUtil.NEWLINE;

        String result = formatter.format(text, fieldName);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveMultipleNewlinesInAbstractField() {
        String fieldName = "abstract";
        // The newlines are normalized according to the globally configured newline setting in the formatter
        String text = "lorem ipsum lorem ipsum" + StringUtil.NEWLINE + StringUtil.NEWLINE + "lorem ipsum lorem ipsum"
                + StringUtil.NEWLINE;

        String result = formatter.format(text, fieldName);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveNewlineInReviewField() {
        String fieldName = "review";
        // The newlines are normalized according to the globally configured newline setting in the formatter
        String text = "lorem ipsum lorem ipsum" + StringUtil.NEWLINE + "lorem ipsum lorem ipsum" + StringUtil.NEWLINE;

        String result = formatter.format(text, fieldName);
        String expected = "{"+text+"}";

        assertEquals(expected, result);
    }

    @Test
    public void removeWhitespaceFromNonMultiLineFields() throws Exception {
        String original = "I\nshould\nnot\ninclude\nadditional\nwhitespaces  \nor\n\ttabs.";
        String expected = "{I should not include additional whitespaces or tabs.}";

        String title = formatter.format(original, "title");
        String any = formatter.format(original, "anyotherfield");

        assertEquals(expected, title);
        assertEquals(expected, any);
    }
}
