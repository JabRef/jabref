package net.sf.jabref.exporter.layout;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.LatexFieldFormatter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by joerg on 05.10.2015.
 */
public class LatexFieldFormatterTests {

    private LatexFieldFormatter formatter;

    @BeforeClass
    public static void setUpBeforeClass(){
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Before
    public void setUp() {
        this.formatter = new LatexFieldFormatter();
    }

    @Test
    public void normalizeNewlineInAbstractField() {
        String fieldName = "abstract";
        String text = "lorem" + Globals.NEWLINE + " ipsum lorem ipsum\nlorem ipsum lorem ipsum\r\ntest";

        // The newlines are normalized according to the globally configured newline setting in the formatter
        String expected = "{" + "lorem" + Globals.NEWLINE + " ipsum lorem ipsum" + Globals.NEWLINE
                + "lorem ipsum lorem ipsum"
                + Globals.NEWLINE + "test" + "}";

        String result = formatter.format(text, fieldName);

        assertEquals(expected, result);
    }

    @Test
    public void preserveNewlineInAbstractField() {
        String fieldName = "abstract";
        String text = "lorem ipsum lorem ipsum\nlorem ipsum lorem ipsum\n";
        // The newlines are normalized according to the globally configured newline setting in the formatter
        // Therefore, "\n" has to be replaced by that
        text = text.replaceAll("\n", Globals.NEWLINE);

        String result = formatter.format(text, fieldName);
        String expected = "{"+text+"}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveMultipleNewlinesInAbstractField() {
        String fieldName = "abstract";
        String text = "lorem ipsum lorem ipsum\n\nlorem ipsum lorem ipsum\n";
        // The newlines are normalized according to the globally configured newline setting in the formatter
        // Therefore, "\n" has to be replaced by that
        text = text.replaceAll("\n", Globals.NEWLINE);

        String result = formatter.format(text, fieldName);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveNewlineInReviewField() {
        String fieldName = "review";
        String text = "lorem ipsum lorem ipsum\nlorem ipsum lorem ipsum\n";
        // The newlines are normalized according to the globally configured newline setting in the formatter
        // Therefore, "\n" has to be replaced by that
        text = text.replaceAll("\n", Globals.NEWLINE);

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
