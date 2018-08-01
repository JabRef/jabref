package org.jabref.logic.bibtex;

import java.util.Collections;

import org.jabref.logic.util.OS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class LatexFieldFormatterTests {

    private LatexFieldFormatter formatter;

    @BeforeEach
    public void setUp() {
        this.formatter = new LatexFieldFormatter(mock(LatexFieldFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS));
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

    public void reportUnbalancedBracing() throws Exception {
        String unbalanced = "{";

        assertThrows(InvalidFieldValueException.class, () -> formatter.format(unbalanced, "anyfield"));
    }

    public void reportUnbalancedBracingWithEscapedBraces() throws Exception {
        String unbalanced = "{\\}";

        assertThrows(InvalidFieldValueException.class, () -> formatter.format(unbalanced, "anyfield"));
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

    @Test
    public void hashEnclosedWordsGetRealStringsInMonthField() throws Exception {
        String text = "#jan# - #feb#";
        assertEquals("jan #{ - } # feb", formatter.format(text, "month"));
    }

    @Test
    public void hashEnclosedWordsGetRealStringsInMonthFieldBecauseMonthIsStandardField() throws Exception {
        LatexFieldFormatterPreferences latexFieldFormatterPreferences = new LatexFieldFormatterPreferences(
                false, Collections.emptyList(), new FieldContentParserPreferences());
        LatexFieldFormatter formatter = new LatexFieldFormatter(latexFieldFormatterPreferences);
        String text = "#jan# - #feb#";
        assertEquals("jan #{ - } # feb", formatter.format(text, "month"));
    }

}
