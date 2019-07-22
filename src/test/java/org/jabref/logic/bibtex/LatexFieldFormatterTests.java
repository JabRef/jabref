package org.jabref.logic.bibtex;

import java.util.Collections;

import org.jabref.logic.util.OS;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

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
        String text = "lorem" + OS.NEWLINE + " ipsum lorem ipsum\nlorem ipsum \rlorem ipsum\r\ntest";

        String expected = "{" + "lorem" + OS.NEWLINE + " ipsum lorem ipsum" + OS.NEWLINE
                + "lorem ipsum "
                + OS.NEWLINE + "lorem ipsum"
                + OS.NEWLINE + "test" + "}";

        String result = formatter.format(text, StandardField.ABSTRACT);

        assertEquals(expected, result);
    }

    @Test
    public void newlineAtEndOfAbstractFieldIsDeleted() throws Exception {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = formatter.format(text + OS.NEWLINE, StandardField.ABSTRACT);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveNewlineInAbstractField() throws Exception {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = formatter.format(text, StandardField.ABSTRACT);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveMultipleNewlinesInAbstractField() throws Exception {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = formatter.format(text, StandardField.ABSTRACT);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveNewlineInReviewField() throws Exception {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = formatter.format(text, StandardField.REVIEW);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    public void removeWhitespaceFromNonMultiLineFields() throws Exception {
        String original = "I\nshould\nnot\ninclude\nadditional\nwhitespaces  \nor\n\ttabs.";
        String expected = "{I should not include additional whitespaces or tabs.}";

        String title = formatter.format(original, StandardField.TITLE);
        String any = formatter.format(original, new UnknownField("anyotherfield"));

        assertEquals(expected, title);
        assertEquals(expected, any);
    }

    @Test
    public void reportUnbalancedBracing() throws Exception {
        String unbalanced = "{";

        assertThrows(InvalidFieldValueException.class, () -> formatter.format(unbalanced, new UnknownField("anyfield")));
    }

    @Test
    public void reportUnbalancedBracingWithEscapedBraces() throws Exception {
        String unbalanced = "{\\}";

        assertThrows(InvalidFieldValueException.class, () -> formatter.format(unbalanced, new UnknownField("anyfield")));
    }

    @Test
    public void tolerateBalancedBrace() throws Exception {
        String text = "Incorporating evolutionary {Measures into Conservation Prioritization}";

        assertEquals("{" + text + "}", formatter.format(text, new UnknownField("anyfield")));
    }

    @Test
    public void tolerateEscapeCharacters() throws Exception {
        String text = "Incorporating {\\O}evolutionary {Measures into Conservation Prioritization}";

        assertEquals("{" + text + "}", formatter.format(text, new UnknownField("anyfield")));
    }

    @Test
    public void hashEnclosedWordsGetRealStringsInMonthField() throws Exception {
        String text = "#jan# - #feb#";
        assertEquals("jan #{ - } # feb", formatter.format(text, StandardField.MONTH));
    }

    @Test
    public void hashEnclosedWordsGetRealStringsInMonthFieldBecauseMonthIsStandardField() throws Exception {
        LatexFieldFormatterPreferences latexFieldFormatterPreferences = new LatexFieldFormatterPreferences(
                false, Collections.emptyList(), new FieldContentParserPreferences());
        LatexFieldFormatter formatter = new LatexFieldFormatter(latexFieldFormatterPreferences);
        String text = "#jan# - #feb#";
        assertEquals("jan #{ - } # feb", formatter.format(text, StandardField.MONTH));
    }
}
