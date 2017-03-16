package org.jabref.logic.bibtex;

import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LatexFieldFormatterTests {

    private LatexFieldFormatter formatter;

    @Before
    public void setUp() {
        this.formatter = new LatexFieldFormatter(JabRefPreferences.getInstance().getLatexFieldFormatterPreferences());
    }

    @Test
    public void normalizeNewlineInAbstractField() throws Exception {
        String fieldName = "abstract";
        String text = "lorem" + OS.NEWLINE + " ipsum lorem ipsum\nlorem ipsum \rlorem ipsum\r\ntest";

        // The newlines are normalized according to the globally configured newline setting in the formatter
        String expected = "{" + "lorem" + OS.NEWLINE + " ipsum lorem ipsum" + OS.NEWLINE
 + "lorem ipsum "
                + OS.NEWLINE + "lorem ipsum"
                + OS.NEWLINE + "test" + "}";

        String result = formatter.format(text, fieldName);

        assertEquals(expected, result);
    }

    @Test
    public void preserveNewlineInAbstractField() throws Exception {
        String fieldName = "abstract";
        // The newlines are normalized according to the globally configured newline setting in the formatter
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum" + OS.NEWLINE;

        String result = formatter.format(text, fieldName);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveMultipleNewlinesInAbstractField() throws Exception {
        String fieldName = "abstract";
        // The newlines are normalized according to the globally configured newline setting in the formatter
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + OS.NEWLINE + "lorem ipsum lorem ipsum"
                + OS.NEWLINE;

        String result = formatter.format(text, fieldName);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveNewlineInReviewField() throws Exception {
        String fieldName = "review";
        // The newlines are normalized according to the globally configured newline setting in the formatter
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum" + OS.NEWLINE;

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

    @Test(expected = InvalidFieldValueException.class)
    public void reportUnbalancedBracing() throws Exception {
        String unbalanced = "{";

        formatter.format(unbalanced, "anyfield");
    }

    @Test(expected = InvalidFieldValueException.class)
    public void reportUnbalancedBracingWithEscapedBraces() throws Exception {
        String unbalanced = "{\\}";

        formatter.format(unbalanced, "anyfield");
    }

    @Test
    public void tolerateBalancedBrace() throws Exception {
        String text = "Incorporating evolutionary {Measures into Conservation Prioritization}";

        assertEquals("{" + text + "}", formatter.format(text, "anyfield"));
    }

    @Test
    public void tolerateEscapeCharacters() throws Exception {
        String text = "Incorporating {\\O}evolutionary {Measures into Conservation Prioritization}";

        assertEquals("{" + text + "}", formatter.format(text, "anyfield"));
    }
}
